(ns metabase.native-query-analyzer
  "Integration with Macaw, which parses native SQL queries. All SQL-specific logic is in Macaw, the purpose of this
  namespace is to:

  1. Translate Metabase-isms into generic SQL that Macaw can understand.
  2. Contain Metabase-specific business logic."
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [macaw.core :as macaw]
   [metabase.config :as config]
   [metabase.native-query-analyzer.parameter-substitution :as nqa.sub]
   [metabase.public-settings :as public-settings]
   [metabase.util :as u]
   [toucan2.core :as t2]))

(def ^:dynamic *parse-queries-in-test?*
  "Normally, a native card's query is parsed on every create/update. For most tests, this is an unnecessary
  expense. Therefore, we skip parsing while testing unless this variable is turned on.

  c.f. [[active?]]"
  false)

(defn- active?
  "Should the query run? Either we're not testing or it's been explicitly turned on.

  c.f. [[*parse-queries-in-test?*]], [[public-settings/sql-parsing-enabled]]"
  []
  (and (public-settings/sql-parsing-enabled)
       (or (not config/is-test?)
           *parse-queries-in-test?*)))

(defn- normalize-name
  ;; TODO: This is wildly naive and will be revisited once the rest of the plumbing is sorted out
  ;; c.f. Milestone 3 of the epic: https://github.com/metabase/metabase/issues/36911
  [name]
  (-> name
      (str/replace "\"" "")
      u/lower-case-en))

(def ^:private field-and-table-fragment
  "HoneySQL fragment to get the Field and Table"
  {:from [[:metabase_field :f]]
   ;; (t2/table-name :model/Table) doesn't work on CI since models/table.clj hasn't been loaded
   :join [[:metabase_table :t] [:= :table_id :t.id]]})

(defn- direct-field-ids-for-query
  "Very naively selects the IDs of Fields that could be used in the query. Improvements to this are planned for Q2 2024,
  c.f. Milestone 3 of https://github.com/metabase/metabase/issues/36911"
  [{column-maps :columns table-maps :tables} db-id]
  (let [columns (map :component column-maps)
        tables (map :component table-maps)]
    (t2/select-pks-set :model/Field (merge field-and-table-fragment
                                           {:where [:and
                                                    [:= :t.db_id db-id]
                                                    (if (seq tables)
                                                      [:in :%lower.t/name (map normalize-name tables)]
                                                      ;; if we don't know what tables it's from, look everywhere
                                                      true)
                                                    (if (seq columns)
                                                      [:in :%lower.f/name (map normalize-name columns)]
                                                      ;; if there are no columns, it must be a select * or similar
                                                      false)]}))))

(defn- indirect-field-ids-for-query
  "Similar to direct-field-ids-for-query, but for wildcard selects"
  [{table-wildcard-maps :table-wildcards
    all-wildcard-maps   :has-wildcard?
    table-maps          :tables}
   db-id]
  (let [table-wildcards           (map :component table-wildcard-maps)
        has-wildcard?             (and (seq all-wildcard-maps)
                                       (reduce #(and %1 %2) true (map :component all-wildcard-maps)))
        tables                    (map :component table-maps)
        active-fields-from-tables
        (fn [table-names]
          (t2/select-pks-set :model/Field (merge field-and-table-fragment
                                                 {:where [:and
                                                          [:= :t.db_id db-id]
                                                          [:= true :f.active]
                                                          [:in :%lower.t/name
                                                               (map normalize-name table-names)]]})))]
    (cond
      ;; select * from ...
      ;; so, get everything in all the tables
      (and has-wildcard? (seq tables)) (active-fields-from-tables tables)
      ;; select foo.* from ...
      ;; limit to the named tables
      (seq table-wildcards)            (active-fields-from-tables table-wildcards))))

(defn field-ids-for-sql
  "Returns a `{:direct #{...} :indirect #{...}}` map with field IDs that (may) be referenced in the given cards's
  query. Errs on the side of optimism: i.e., it may return fields that are *not* in the query, and is unlikely to fail
  to return fields that are in the query.

  Direct references are columns that are named in the query; indirect ones are from wildcards. If a field could be
  both direct and indirect, it will *only* show up in the `:direct` set."
  [query]
  (when (and (active?)
             (:native query))
    (let [db-id        (:database query)
          sql-string   (:query (nqa.sub/replace-tags query))
          parsed-query (macaw/query->components (macaw/parsed-query sql-string))
          direct-ids   (direct-field-ids-for-query parsed-query db-id)
          indirect-ids (set/difference
                        (indirect-field-ids-for-query parsed-query db-id)
                        direct-ids)]
      {:direct   direct-ids
       :indirect indirect-ids})))

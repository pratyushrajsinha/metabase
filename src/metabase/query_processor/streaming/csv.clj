(ns metabase.query-processor.streaming.csv
  (:require
   [clojure.data.csv]
   [java-time.api :as t]
   [medley.core :as m]
   [metabase.formatter :as formatter]
   [metabase.query-processor.pivot.postprocess :as qp.pivot.postprocess]
   [metabase.query-processor.streaming.common :as common]
   [metabase.query-processor.streaming.interface :as qp.si]
   [metabase.shared.models.visualization-settings :as mb.viz]
   [metabase.util.date-2 :as u.date]
   [metabase.util.performance :as perf])
  (:import
   (java.io BufferedWriter OutputStream OutputStreamWriter)
   (java.nio.charset StandardCharsets)))

(set! *warn-on-reflection* true)

(defmethod qp.si/stream-options :csv
  ([_]
   (qp.si/stream-options :csv "query_result"))
  ([_ filename-prefix]
   {:content-type              "text/csv"
    :status                    200
    :headers                   {"Content-Disposition" (format "attachment; filename=\"%s_%s.csv\""
                                                              (or filename-prefix "query_result")
                                                              (u.date/format (t/zoned-date-time)))}
    :write-keepalive-newlines? false}))

;; As a first step towards hollistically solving this issue: https://github.com/metabase/metabase/issues/44556
;; (which is basically that very large pivot tables can crash the export process),
;; The post processing is disabled completely.
;; This should remain `false` until it's fixed
;; TODO: rework this post-processing once there's a clear way in app to enable/disable it, or to select alternate download options
(def ^:dynamic *pivot-export-post-processing-enabled*
  "Flag to enable/disable export post-processing of pivot tables.
  Disabled by default and should remain disabled until Issue #44556 is resolved and a clear plan is made."
  false)

(defn- write-csv
  "Custom implementation of `clojure.data.csv/write-csv` with a more efficient quote? predicate and no support for
  options (we don't use them)."
  [writer data]
  (let [separator \,
        quote \"
        quote? (fn [^String s]
                 (let [n (.length s)]
                   (loop [i 0]
                     (if (>= i n) false
                         (let [ch (.charAt s (unchecked-int i))]
                           (if (or (= ch \,) ;; separator
                                   (= ch \") ;; quote
                                   (= ch \return)
                                   (= ch \newline))
                             true
                             (recur (unchecked-inc i))))))))
        newline "\n"]
    (#'clojure.data.csv/write-csv* writer data separator quote quote? newline)))

;; Rebind write-cell to avoid using clojure.core/escape. Instead, use String.replace with known arguments (we never
;; change quote symbol anyway).
(.bindRoot #'clojure.data.csv/write-cell
           (fn [^java.io.Writer writer obj _ _ quote?]
             (let [^String string (str obj)
                   must-quote (quote? string)]
               (when must-quote (.write writer "\""))
               (.write writer (if must-quote
                                (.replace string "\"" "\"\"")
                                string))
               (when must-quote (.write writer "\"")))))

(defmethod qp.si/streaming-results-writer :csv
  [_ ^OutputStream os]
  (let [writer             (BufferedWriter. (OutputStreamWriter. os StandardCharsets/UTF_8))
        ordered-formatters (volatile! nil)
        pivot-data         (atom nil)
        pivot-grouping     (atom nil)]
    (reify qp.si/StreamingResultsWriter
      (begin! [_ {{:keys [ordered-cols results_timezone format-rows? pivot-export-options]
                   :or   {format-rows? true}} :data} viz-settings]
        (let [opts               (when (and *pivot-export-post-processing-enabled* pivot-export-options)
                                   (-> (merge {:pivot-rows []
                                               :pivot-cols []}
                                              pivot-export-options)
                                       (assoc :column-titles (common/column-titles ordered-cols (::mb.viz/column-settings viz-settings) format-rows?))
                                       qp.pivot.postprocess/add-pivot-measures))
              ;; col-names are created later when exporting a pivot table, so only create them if there are no pivot options
              col-names          (when-not opts (common/column-titles ordered-cols (::mb.viz/column-settings viz-settings) format-rows?))
              pivot-grouping-key (qp.pivot.postprocess/pivot-grouping-key col-names)]
          (when opts
            (reset! pivot-data (qp.pivot.postprocess/init-pivot opts)))
          ;; when we have a pivot-grouping, but no opts, we still want to use that to 'clean up' the raw pivot rows
          (when-not opts
            (reset! pivot-grouping pivot-grouping-key ))
          (vreset! ordered-formatters
                   (if format-rows?
                     (mapv #(formatter/create-formatter results_timezone % viz-settings) ordered-cols)
                     (vec (repeat (count ordered-cols) identity))))
          ;; write the column names for non-pivot tables
          (when col-names
            (let [row (m/remove-nth pivot-grouping-key col-names)]
              (csv/write-csv writer [row])
              (.flush writer)))))

      (write-row! [_ row _row-num _ {:keys [output-order]}]
        (let [ordered-row (if output-order
                            (let [row-v (into [] row)]
                              (into [] (for [i output-order] (row-v i))))
                            row)]
          (if @pivot-data
            ;; if we're processing a pivot result, we don't write it out yet, just aggregate it
            ;; so that we can post process the data in finish!
            (when (= 0 (nth ordered-row (get-in @pivot-data [:config :pivot-grouping])))
              (swap! pivot-data (fn [a] (qp.pivot.postprocess/add-row a ordered-row))))
            (let [pivot-grouping-key @pivot-grouping
                  group              (get ordered-row pivot-grouping-key)]
              (when (= 0 group)
                (let [formatted-row (cond->> (mapv (fn [formatter r]
                                                     (formatter (common/format-value r)))
                                                   @ordered-formatters ordered-row)
                                      pivot-grouping-key (m/remove-nth pivot-grouping-key))]
                  (csv/write-csv writer [formatted-row])
                  (.flush writer)))))))

      (finish! [_ _]
        ;; TODO -- not sure we need to flush both
        (when @pivot-data
          (doseq [xf-row (qp.pivot.postprocess/build-pivot-output @pivot-data @ordered-formatters)]
            (csv/write-csv writer [xf-row])))
        (.flush writer)
        (.flush os)
        (.close writer)))))

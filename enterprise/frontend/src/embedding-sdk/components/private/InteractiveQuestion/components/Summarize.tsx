import { AggregationItemList } from "metabase/query_builder/components/view/sidebars/SummarizeSidebar/SummarizeContent";
import type * as Lib from "metabase-lib";

import { useInteractiveQuestionContext } from "../context";

/*type SummarizeProps = {
  onClose: () => void;
};*/

export const Summarize = () => {
  const { question, updateQuestion } = useInteractiveQuestionContext();

  const onQueryChange = (query: Lib.Query) => {
    if (question) {
      return updateQuestion(question.setQuery(query));
    }
  };

  if (!question) {
    return null;
  }

  return (
    <AggregationItemList
      query={question.query()}
      onQueryChange={onQueryChange}
    />
  );
};

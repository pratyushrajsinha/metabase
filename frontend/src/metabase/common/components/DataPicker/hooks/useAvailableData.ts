import { useSearchQuery } from "metabase/api";
import type { DatabaseId } from "metabase-types/api";

interface Props {
  databaseId?: DatabaseId;
}

export const useAvailableData = ({ databaseId }: Props = {}) => {
  const { data, isLoading } = useSearchQuery(
    {
      context: "available-models",
      limit: 0,
      models: ["card"],
      table_db_id: databaseId,
      calculate_available_models: true,
    },
    {
      refetchOnMountOrArgChange: true,
    },
  );
  const availableModels = data?.available_models ?? [];
  const hasQuestions = availableModels.includes("card");
  const hasModels = availableModels.includes("dataset");
  const hasMetrics = availableModels.includes("metric");

  return {
    isLoading,
    hasQuestions,
    hasModels,
    hasMetrics,
  };
};

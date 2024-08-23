import { useState } from "react";
import { t } from "ttag";

import { ViewFooterButton } from "metabase/components/ViewFooterButton";
import { PLUGIN_FEATURE_LEVEL_PERMISSIONS } from "metabase/plugins";
import { Flex, Popover } from "metabase/ui";
import type Question from "metabase-lib/v1/Question";
import type {
  DashCardId,
  DashboardId,
  Dataset,
  VisualizationSettings,
} from "metabase-types/api";

import { QueryDownloadPopover } from "../QueryDownloadPopover";
import { useDownloadData } from "../QueryDownloadPopover/use-download-data";

interface QueryDownloadWidgetProps {
  className?: string;
  question: Question;
  result: Dataset;
  uuid?: string;
  token?: string;
  visualizationSettings?: VisualizationSettings;
  dashcardId?: DashCardId;
  dashboardId?: DashboardId;
}

const QueryDownloadWidget = ({
  className,
  question,
  result,
  dashboardId,
  dashcardId,
  uuid,
  token,
  visualizationSettings,
}: QueryDownloadWidgetProps) => {
  const [isPopoverOpen, setIsPopoverOpen] = useState(false);

  const [, handleDownload] = useDownloadData({
    question,
    result,
    dashboardId,
    dashcardId,
    uuid,
    token,
    visualizationSettings,
  });

  return (
    <Popover opened={isPopoverOpen} onClose={() => setIsPopoverOpen(false)}>
      <Popover.Target>
        <Flex className={className}>
          <ViewFooterButton
            icon="download"
            data-testid="download-button"
            tooltipLabel={t`Download full results`}
            onClick={() => setIsPopoverOpen(!isPopoverOpen)}
          />
        </Flex>
      </Popover.Target>
      <Popover.Dropdown p="0.75rem">
        <QueryDownloadPopover
          question={question}
          result={result}
          onDownload={opts => {
            setIsPopoverOpen(false);
            handleDownload(opts);
          }}
        />
      </Popover.Dropdown>
    </Popover>
  );
};

interface QueryDownloadWidgetOpts {
  result?: Dataset;
}

QueryDownloadWidget.shouldRender = ({ result }: QueryDownloadWidgetOpts) => {
  return (
    result &&
    !result.error &&
    PLUGIN_FEATURE_LEVEL_PERMISSIONS.canDownloadResults(result)
  );
};

// eslint-disable-next-line import/no-default-export -- deprecated usage
export default QueryDownloadWidget;

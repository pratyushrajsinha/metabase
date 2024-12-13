---
title: Questions
redirect_from:
  - /docs/latest/users-guide/04-asking-questions
  - /docs/latest/users-guide/06-sharing-answers
---

# Questions

Questions in Metabase are queries, their results, and their visualization. You can think about them as saved queries that you can display as tables or charts, and organize into collections and dashboards.

## Creating a new question

You can create a new question from scratch, or build off of an existing question.

### From scratch

To create a question from scratch, you can click on **+ New** and select how you want to query your data:

- [Question](./query-builder/editor.md), which takes you to the graphical query builder.
- [SQL/native code](./native-editor/writing-sql.md), which takes you to the native code editor.

Even if you know SQL, you should check out the [graphical query builder](./query-builder/editor.md), as you can use it to build interactive charts.

### From an existing question

You can also build a new question from an existing question. You won't overwrite the existing question, so feel free to play around. You can use either the [query builder](./query-builder/editor.md) or the [native code editor](./native-editor/writing-sql.md).

## Saving questions to dashboards or collections

Once you've built your query and [visualized its results], you can save a question to a [dashboard](../../dashboards/introduction.md) (the default), or to a [collection](../../exploration-and-organization/collections.md). You'll need to name the question, and include an optional description.

### Saving questions to dashboards

Questions that live in a dashboard are only visible in that dashboard. They can't be used in other dashboards.

### Saving questions to collections

Questions saved to a collection can be added to multiple dashboards. Moving a question from one collection to another collection won't have any effect on the dashboards the question has been added to. In order to save a question to a collection, you'll need to be in a group with [curate access](../permissions/collections.md#curate-access) to that collection.

## Moving questions from collections to dashboards (and vice versa)

Whether you can move a question in a collection into a dashboard depends on how many other dashboards use that question.

You can move the question from a collection into a dashboard if:

- No other dashboards use that question.
- The other dashboards that use that question live in collections you have curate access to. In this case, Metabase will tell you which other dashboards use that question, and you'll have to decide whether you're okay with removing the question from those dashboards.

If, however, dashboards that use that question are in collections that you lack curate access to, you won't be able to move that question into a dashboard.

## Bookmark a question

Click the **bookmark** icon to pin a question to your Metabase sidebar. See [Bookmarks](../../exploration-and-organization/exploration.md#bookmarks).

## Info about your question

Once you save a question, you can click on the **info** icon in the upper right to see some metadata about your question:

![Info sidesheet](./images/info-sidesheet.png)

### Overview tab

- Description, which you can add–descriptions even support Markdown!
- Who created the question, and who edited it last
- The collection or dashboard the question is saved in
- The data the question is based on.
- The question's Entity ID (which you can use with [Serialization](../../installation-and-operation/serialization.md) to keep IDs consistent across multiple Metabases).

### History tab

See [History](../../exploration-and-organization/history.md).

## Downloading your question's results

See [exporting results](./exporting-results.md).

## Verifying a question

See [content verification](../exploration-and-organization/content-verification.md).

## Turning a question into a model

You can turn a question into a model to let others know that the results make a good starting point for new questions. See [models](../data-modeling/models.md).

## Caching question results

{% include plans-blockquote.html feature="Caching question results" %}

See [Caching per question](../../configuring-metabase/caching.md#question-caching-policy).

## Setting up alerts

You can set up questions to run periodically and notify you if the results are interesting. Check out [alerts](./alerts.md).

## Viewing events on your chart

If your results are a time series, you can display events. See [Events and timelines](../exploration-and-organization/events-and-timelines.md).

## Deleting a question

See [delete and restore](../exploration-and-organization/delete-and-restore.md).
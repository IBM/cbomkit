<template>
  <div>
    <div v-for="(error, rowIndex) in model.errors" :key="rowIndex">
      <div class="notificationContainer">
        <cv-toast-notification
          :kind="errorComponents(error).kind"
          :title="errorComponents(error).title"
          :sub-title="errorComponents(error).description"
          close-aria-label="Close"
          @close="model.closeError(rowIndex)"
          :style="{ position: 'relative', top: verticalGap(rowIndex) + 'px' }"
        >
        </cv-toast-notification>
      </div>
    </div>
  </div>
</template>

<script>
import { model, ErrorStatus } from "@/model.js";

export default {
  name: "NotificationsView",
  data() {
    return {
      model,
    };
  },
  methods: {
    errorComponents(error) {
      var kind = "error";
      var title = "Unknown error";
      var description = "An unknown error has occured.";
      if (error.status === ErrorStatus.NoConnection) {
        kind = "error";
        title = "No connection";
        description =
          "Connection to the server has failed. Please try again later.";
      } else if (error.status === ErrorStatus.InvalidRepo) {
        kind = "error";
        title = "Invalid repository";
        description =
          "The provided address does not lead to a readable repository.";
      } else if (error.status === ErrorStatus.JsonParsing) {
        kind = "error";
        title = "Parsing error";
        description = "An incorrect JSON file cannot be parsed.";
      } else if (error.status === ErrorStatus.ScanError) {
        kind = "error";
        title = "Error while scanning";
        description = error.message;
      } else if (error.status === ErrorStatus.InvalidCbom) {
        kind = "error";
        title = "Invalid CBOM";
        description = "The provided CBOM does not respect the expected format.";
      } else if (error.status === ErrorStatus.IgnoredComponent) {
        kind = "info";
        title = "Some components are not shown";
        description = "The provided CBOM contains one or several components that are not cryptographic assets. They are not displayed here.";
      } else if (error.status === ErrorStatus.MultiUpload) {
        kind = "error";
        title = "Multiple upload";
        description = "Please only upload a single CBOM file.";
      } else if (error.status === ErrorStatus.EmptyDatabase) {
        kind = "warning";
        title = "Empty database";
        description =
          "Connection to the server was successful, but the CBOM database is empty.";
      } else if (error.status === ErrorStatus.FallBackLocalComplianceReport) {
        kind = "warning";
        title = "Limited compliance results";
        description =
          "An error occured with the remote compliance service, we fall back on a local compliance report instead, which may be less detailed.";
      }
      return { kind: kind, title: title, description: description };
    },
    verticalGap: function (index) {
      // Spacing things more to make all notifications readable create a weird bug where the first one cannot be erased
      return index * 37;
    },
  },
};
</script>

<style>
.notificationContainer {
  position: fixed;
  right: 0;
  z-index: 1;
  display: flex;
  flex-direction: column;
}
</style>

<template>
  <div>
    <!-- Remove some margin-top to counterbalance some weird spacing of the file uploader component -->
    <cv-file-uploader
      style="margin-top: -24px;"
      ref="test"
      v-model="uploadedFiles"
      v-on:change="loadFiles"
      kind="drag-target"
      accept=".json"
      :clear-on-reselect="true"
      :initial-state-uploading="true"
      :multiple="false"
      :removable="true"
    >
      <template slot="drop-target">
        <div :class="isViewerOnly ? 'drop-container-viewer' : 'drop-container-generator'">
          <div class="description-container">
            <div :class="isViewerOnly ? 'description-header-viewer' : 'description-header-generator'">
              <CloudUpload24 style="margin-right: 8px" />
              <div>
                {{ title }}
              </div>
            </div>
            <div :class="isViewerOnly ? 'description-subheader-viewer' : 'description-subheader-generator'">
              {{ subtitle }}
            </div>
          </div>
        </div>
      </template>
    </cv-file-uploader>
    
  </div>
</template>

<script>
import { model, ErrorStatus } from "@/model.js";
import { isViewerOnly } from "@/helpers.js";
import { CloudUpload24 } from "@carbon/icons-vue";
import { showResultFromUpload } from "@/helpers";

export default {
  name: "FileUploader",
  data() {
    return {
      model,
      uploadedFiles: [],
      subtitle: "(or click to browse)"
    };
  },
  components: {
    CloudUpload24,
  },
  computed: {
    isViewerOnly,
    title() {
      return isViewerOnly() ? "Drop a CBOM here to visualize it" : "Drop a CBOM here";
    }
  },
  methods: {
    loadFiles: function (filesInfo) {
      if (filesInfo.length == 2 && filesInfo[0].state == "") {
        // Only case where we accept two uploaded documents: if the first uploaded document was invalid, we accept that the user uploads a second document, that will replace the first one
        this.uploadedFiles.shift();
      }
      this.fileReader = new FileReader();
      this.fileReader.readAsText(this.uploadedFiles[0].file);
      this.fileReader.addEventListener("load", this.onLoadingComplete);
    },
    onLoadingComplete: function () {
      if (this.uploadedFiles.length != 1) {
        // Allows uploading only a single document
        model.addError(ErrorStatus.MultiUpload);
        console.error("Error: more than one document has been uploaded");
        this.uploadedFiles = [];
        return;
      }
      try {
        let cbom = JSON.parse(this.fileReader.result);
        let name = this.uploadedFiles[0].file.name;
        console.log(`Uploaded CBOM '${name}':`, cbom);
        this.$refs.test.setState(0, "complete");
        showResultFromUpload(cbom, name);
      } catch (error) {
        let file = this.uploadedFiles[0].file;
        console.log(file);
        this.$refs.test.setInvalidMessage(
          0,
          `Please upload a valid JSON file.`
        );
        this.$refs.test.setState(0, "");
        console.error("Error reading uploaded file");
        // The error is displayed by the component, no need to add a notification
      }
    },
  },
};
</script>

<style scoped>
.drop-container-generator {
  display: flex;
  min-height: 60px;
  margin: auto;
}
.drop-container-viewer {
  display: flex;
  min-height: 180px;
  margin: auto;
}
.description-container {
  margin: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.description-header-generator {
  display: flex;
  align-items: center;
  font-size: large;
  font-weight: 400;
}
.description-subheader-generator {
  font-size: small;
  font-weight: 400;
  margin-top: 4px;
}
.description-header-viewer {
  display: flex;
  align-items: center;
  font-size: x-large;
  font-weight: 400;
}
.description-subheader-viewer {
  font-size: medium;
  font-weight: 400;
  margin-top: 4px;
}
</style>

<!-- Allows cv-file-uploader to fully extend horizontally, and decreases its vertical size  -->
<style>
.bx--file-browse-btn {
  max-width: none !important;
}
.bx--file__drop-container {
  height: auto !important;
  background-color: var(--cds-layer);
}
</style>
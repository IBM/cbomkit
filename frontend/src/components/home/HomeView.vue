<template>
  <div>
    <div class="heading-outer">
      <div class="heading-inner">
        <h1 style="font-weight: 400; padding-bottom: 6px">
          {{ getTitle }}
        </h1>
        <h4>
          Explore the use of cryptography in software with
          <cv-link
            :style="{
              color: model.useDarkMode ? 'white' : 'black',
              cursor: 'pointer',
            }"
            href="https://cyclonedx.org/capabilities/cbom/"
            target="_blank"
          >
            <h4>Cryptography Bills of Materials (CBOM)</h4>
          </cv-link>
        </h4>
      </div>
    </div>
    <div v-if="isViewerOnly">
      <FileUploader class="file-uploader-viewer"/>
      <TrySampleButton class="try-sample-button"/>
      <PluginExplainerView class="plugin-explainer-view"/>
    </div>
    <div v-else>
      <SearchOrUploadView class="search-or-upload-view"/>
    </div>
    <ExplainerView class="explainer-view" />
  </div>
</template>

<script>
import { model } from "@/model.js";
import { getTitle, isViewerOnly } from "@/helpers.js";
import SearchOrUploadView from "@/components/home/SearchOrUploadView.vue";
import FileUploader from "@/components/home/FileUploader.vue";
import ExplainerView from "@/components/home/ExplainerView.vue";
import PluginExplainerView from "@/components/home/PluginExplainerView.vue";
import TrySampleButton from "@/components/home/TrySampleButton.vue";

export default {
  name: "HomeView",
  components: {
    FileUploader,
    SearchOrUploadView,
    ExplainerView,
    PluginExplainerView,
    TrySampleButton
  },
  data() {
    return {
      model,
    };
  },
  computed: {
    getTitle,
    isViewerOnly
  }
};
</script>

<style scoped>
.heading-outer {
  text-align: center;
  padding: 5%;
}
.heading-inner {
  display: inline-block;
}
.file-uploader-viewer {
  padding: 0% 5%;
}
.try-sample-button {
  padding: 0% 5%;
  margin-top: -8px
}
.plugin-explainer-view {
  padding: 3% 5%;
}
.search-or-upload-view {
  padding: 3% 5%;
}
.explainer-view {
  padding: 0% 5% 7%;
}
</style>

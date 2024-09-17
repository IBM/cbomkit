<template>
  <div>
    <!-- TODO: add a progress bar? -->
    <!-- <progress-bar></progress-bar> -->
    <cv-inline-loading
      v-if="model.scanning.scanningStatus != null"
      ending-text="Scan ended"
      :error-text="model.scanning.scanningStatusMessage"
      :loading-text="model.scanning.scanningStatusMessage"
      loaded-text="Scan finished"
      :state="model.scanning.scanningStatus"
    ></cv-inline-loading>
  </div>
</template>

<script>
import { model } from "@/model.js";
// import ProgressBar from 'vue-simple-progress' // https://github.com/dzwillia/vue-simple-progress

export default {
  name: "LoaderView",
  components: {
    // ProgressBar
  },
  data() {
    return {
      model,
    };
  },
};
</script>

<template>
  <div>
    <cv-tile style="padding: 0px">
      <div style="padding: 16px; padding-bottom: 8px">
        <div>
          <h3 style="padding-bottom: 6px; font-weight: 500">
            {{ dataTableTitle }}
          </h3>
          <h4 style="padding-bottom: 8px" v-html="dataTableSubtitle"></h4>
          <cv-tag v-if="showLink" :label="linkLabel" />
          <cv-tag v-if="showBranch" :label="branchLabel" />
          <cv-tag v-if="showCommitID" :label="commitIDLabel" />
          <cv-tag v-if="showSubfolder" :label="subfolderLabel" />
          <cv-tag
            v-for="purl in model.codeOrigin.purls"
            :key="purl"
            :label="purl"
          ></cv-tag>
        </div>
      </div>
      <div v-if="getDetections().length > 0 || model.scanning.isScanning">
        <RegulatorResults style="padding-top: 12px" />
        <StatisticsView style="padding: 22px 16px" />
      </div>
    </cv-tile>
  </div>
</template>

<script>
import { model } from "@/model.js";
import { getDetections, numberFormatter, formatSeconds } from "@/helpers";
import RegulatorResults from "@/components/results/RegulatorResults.vue";
import StatisticsView from "@/components/results/StatisticsView.vue";

export default {
  name: "ResultsTitle",
  data() {
    return {
      model,
    };
  },
  components: {
    RegulatorResults,
    StatisticsView,
  },
  computed: {
    showLink() {
      return model.codeOrigin.gitUrl != null;
    },
    linkLabel() {
      return "gitUrl: " + model.codeOrigin.gitUrl;
    },
    showBranch() {
      return model.codeOrigin.revision != null;
    },
    branchLabel() {
      return "revision: " + model.codeOrigin.revision;
    },
    showCommitID() {
      return model.codeOrigin.commitID != null;
    },
    commitIDLabel() {
      return "commit: " + model.codeOrigin.commitID.slice(0, 7) + "...";
    },
    showSubfolder() {
      return model.codeOrigin.subfolder != null;
    },
    subfolderLabel() {
      return "subfolder: " + model.codeOrigin.subfolder;
    },
    dataTableTitle() {
      var title = "Unknown CBOM";
      if (model.codeOrigin.uploadedFileName != null) {
        title = model.codeOrigin.uploadedFileName + " (uploaded)";
      }
      if (model.codeOrigin.projectIdentifier != null) {
        title = model.codeOrigin.projectIdentifier;
      } else {
        title = model.codeOrigin.scanUrl.replace("https://", "");
      }
      return title;
    },
    dataTableSubtitle() {
      let textColor = ""; //model.useDarkMode ? "#4589ff" : "#002d9c"
      let fontWeight = 500;

      var title = "";
      if (
        model.scanning.isScanning &&
        model.scanning.liveDetections.length === 0
      ) {
        title = "Scanning code for cryptographic assets...";
      } else if (
        model.scanning.isScanning &&
        model.scanning.liveDetections.length > 0
      ) {
        title = `<span style="color: ${textColor}; font-weight: ${fontWeight};">${model.scanning.liveDetections.length}</span> cryptographic assets found...`;
      } else if (getDetections().length > 1) {
        title = `<span style="color: ${textColor}; font-weight: ${fontWeight};">${
          getDetections().length
        }</span> cryptographic assets found.`;
      } else if (getDetections().length === 1) {
        title = `<span style="color: ${textColor}; font-weight: ${fontWeight};">${
          getDetections().length
        }</span> cryptographic asset found.`;
      } else {
        title = "No cryptographic asset has been found.";
      }
      if (model.scanning.numberOfFiles && model.scanning.numberOfLines) {
        title += ` Scanned <span style="color: ${textColor}; font-weight: ${fontWeight};">${numberFormatter(
          model.scanning.numberOfLines
        )}</span> ${
          model.scanning.numberOfLines > 1 ? "lines" : "line"
        } of code across <span style="color: ${textColor}; font-weight: ${fontWeight};">${numberFormatter(
          model.scanning.numberOfFiles
        )}</span> ${model.scanning.numberOfFiles > 1 ? "files" : "file"}.`;
      }
      if (model.scanning.totalDuration) {
        var timeSentence = "";
        if (model.scanning.scanDuration) {
          timeSentence += ` Took <span style="color: ${textColor}; font-weight: ${fontWeight};">${formatSeconds(
            model.scanning.scanDuration
          )}</span> to scan (<span style="color: ${textColor}; font-weight: ${fontWeight};">${formatSeconds(
            model.scanning.totalDuration
          )}</span> in total).`;
        } else {
          timeSentence += ` Took <span style="color: ${textColor}; font-weight: ${fontWeight};">${formatSeconds(
            model.scanning.totalDuration
          )}</span> in total.`;
        }
        title += timeSentence;
      }
      // TODO: make it actually refresh each second
      // else if (model.scanning.isScanning) {
      //     let currentTime = new Date()
      //     title += ` Taking <span style="color: ${textColor}; font-weight: ${fontWeight};">${formatSeconds(currentTime - model.scanning.startTime)}</span>.`
      // }
      return `<h4>${title}</h4>`;
    },
  },
  methods: {
    getDetections,
  },
};
</script>

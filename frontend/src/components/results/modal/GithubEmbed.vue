<template>
  <!-- The key below makes vue refresh the component when the link changes -->
  <div :key="embededLink">
    <!-- Code snippet -->
    <component :is="'script'" :src="embededLink" async></component>
    <!-- Placeholder -->
    <div v-if="!embededLink" class="placeholder-code">
      <cv-button kind="ghost" v-on:click="$emit('open-code')">
        Specify the branch to display a code snippet
      </cv-button>
    </div>
  </div>
</template>

<script>
import {model} from "@/model.js";
import {canOpenOnline} from "@/helpers";

export default {
  name: "GithubEmbed",
  data() {
    return {
      model,
      numberOfLinesBeforeAfter: 4,
    };
  },
  props: {
    asset: null,
  },
  computed: {
    embededLink() {
      // TODO: This is a bit duplicated with the helper function to open online, needs refactoring
      if (!canOpenOnline() || this.asset == null) {
        return;
      }

      let gitUrl = model.codeOrigin.gitLink;
      let branch = model.codeOrigin.gitBranch;

      const occurrences = this.asset.evidence.occurrences;
      if (occurrences.length === 1) {
        const firstEntry = occurrences[0];
        const filePath = firstEntry.location;
        const lineNumber = firstEntry.line;
        var codeUrl;
        if (gitUrl.includes("github.com")) {
          codeUrl =
            gitUrl +
            "/blob/" +
            branch +
            "/" +
            filePath +
            "#L" +
            (lineNumber - this.numberOfLinesBeforeAfter) +
            "-" +
            (lineNumber + this.numberOfLinesBeforeAfter);
          let theme = model.useDarkMode ? "github-dark" : "github";
          return `https://emgithub.com/embed-v2.js?target=${encodeURIComponent(
              codeUrl
          )}&style=${theme}&type=code&showBorder=on&showLineNumbers=on&showFullPath=on`;
        }
        return "";
      }
      return "";
    },
  },
};
</script>

<style scoped>
.placeholder-code {
  padding: 70px 0px;
  display: flex;
  justify-content: center;
  background-color: var(--cds-ui-background);
  border: 1px solid gray;
  border-radius: 5px;
  margin-top: 14px;
}
</style>

<style>
/* Makes the 4th line (the actual detection) of the code snippet yellow */
.code-area pre code table tbody tr:nth-child(5) {
  background-color: rgba(255, 255, 0, 0.22);
}
</style>

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
import {getCodeLink} from "@/helpers";

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
      let codeUrl = getCodeLink(this.asset, this.numberOfLinesBeforeAfter);
      if (codeUrl !== undefined && codeUrl !== null && codeUrl.includes("github.com")) {
        let theme = model.useDarkMode ? "github-dark" : "github";
          return `https://emgithub.com/embed-v2.js?target=${encodeURIComponent(
              codeUrl
          )}&style=${theme}&type=code&showBorder=on&showLineNumbers=on&showFullPath=on`;
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

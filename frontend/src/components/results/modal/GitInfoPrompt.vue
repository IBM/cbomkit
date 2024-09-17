<template>
  <div ref="modalPrompt">
    <cv-text-input
      class="input"
      label="Link"
      placeholder="Specify the link to the Git repository associated with this CBOM"
      v-model="gitLink"
      @keyup.enter="confirm()"
    />
    <cv-text-input
      class="input"
      label="Branch"
      placeholder="Specify the branch of the Git repository associated with this CBOM"
      v-model="gitBranch"
      @keyup.enter="confirm()"
    />
    <cv-button
      class="confirm"
      :icon="ArrowRight24"
      @click="confirm()"
      :disabled="!gitLink || !gitBranch"
      >Confirm</cv-button
    >
  </div>
</template>

<script>
import { model } from "@/model.js";
import { ArrowRight24 } from "@carbon/icons-vue";

export default {
  name: "GitInfoPrompt",
  data() {
    return {
      model,
      ArrowRight24,
      gitLink: "",
      gitBranch: "",
    };
  },
  methods: {
    confirm: function () {
      if (this.gitLink && this.gitBranch) {
        model.codeOrigin.gitLink = this.gitLink;
        model.codeOrigin.gitBranch = this.gitBranch;
        this.$emit("confirm-prompt");
      }
    },
  },
  beforeMount() {
    // Executed on page load
    this.gitLink = model.codeOrigin.gitLink;
    this.gitBranch = model.codeOrigin.gitBranch;
  },
};
</script>

<style scoped>
.confirm {
  margin-top: 10px;
}
.input {
  padding-bottom: 10px;
}
</style>

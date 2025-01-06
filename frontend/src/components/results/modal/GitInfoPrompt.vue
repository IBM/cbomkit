<template>
  <div ref="modalPrompt">
    <cv-text-input
      class="input"
      label="Link"
      placeholder="Specify the link to the Git repository associated with this CBOM"
      v-model="gitLink"
      @keyup.enter="confirm()"
    />
    <cv-radio-group :vertical="false" style="padding: 18px 0px 8px">
      <cv-radio-button 
        name="group-1" 
        label="Commit ID (recommended)" 
        value="commitID" 
        v-model="selectedOption"
      />
      <cv-radio-button 
        name="group-1" 
        label="Branch" 
        value="branch" 
        v-model="selectedOption"
      />
    </cv-radio-group>
    <cv-text-input
      v-if="selectedOption === 'commitID'"
      class="input"
      label="Commit ID"
      placeholder="Specify the commit ID of the Git repository associated with this CBOM"
      v-model="commitID"
      @keyup.enter="confirm()"
    />
    <cv-text-input
      v-if="selectedOption === 'branch'"
      class="input"
      label="Branch"
      placeholder="Specify the branch of the Git repository associated with this CBOM"
      v-model="gitBranch"
      @keyup.enter="confirm()"
    />
    <cv-button
      class="confirm"
      style="margin-top: 24px;"
      :icon="ArrowRight24"
      @click="confirm()"
      :disabled="!gitLink || (selectedOption === 'branch' && !gitBranch) || (selectedOption === 'commitID' && !commitID)"
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
      commitID: "",
      selectedOption: "",
    };
  },
  methods: {
    confirm: function () {
      if (this.gitLink && (this.gitBranch || this.commitID)) {
        model.codeOrigin.gitUrl = this.gitLink;
        if (this.selectedOption === "branch") {
          model.codeOrigin.revision = this.gitBranch;
          model.codeOrigin.commitID = null; // remove the commit ID
        }
        if (this.selectedOption === "commitID") {
          model.codeOrigin.commitID = this.commitID;
          model.codeOrigin.revision = null; // remove the branch
        }
        this.$emit("confirm-prompt");
      }
    },
    resetModal: function () {
      this.gitLink = model.codeOrigin.scanUrl;
      this.gitBranch = model.codeOrigin.revision;
      this.commitID = model.codeOrigin.commitID;

      // If the CBOM contains a branch but not a commit ID, show the branch by default
      if (this.gitBranch && !this.commitID) {
        this.selectedOption = "branch";
      } else {
        this.selectedOption = "commitID";
      }
    }
  },
  beforeMount() {
    this.resetModal();
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

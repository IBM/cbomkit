<template>
  <div>
    <div class="first-line">
      <ReturnButton class="return-button" />
    </div>
    <ResultTitle class="result-title" />
    <DataTable class="data-table" />
  </div>
</template>

<script>
import { model } from "@/model.js";
import DataTable from "@/components/results/DataTable.vue";
import ResultTitle from "@/components/results/ResultTitle.vue";
import ReturnButton from "@/components/results/ReturnButton.vue";

export default {
  name: "ResultsView",
  components: {
    DataTable,
    ResultTitle,
    ReturnButton,
  },
  data() {
    return {
      model,
    };
  },
};
</script>

<style scoped>
.first-line {
  padding: 3% 5% 0%;
  display: flex;
  align-items: center;
}

.loader-view {
  flex: 1;
  padding-left: 1%;
}
.result-title {
  padding: 0% 5% 3%;
}
.data-table {
  padding: 0% 5%;
  padding-bottom: 100px;
}
.return-button {
  flex: 1;
  text-align: right;
}
</style>

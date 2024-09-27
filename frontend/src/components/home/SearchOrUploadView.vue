<template>
    <div>
        <div class="list-table">
            <div class="icon-and-title-container">
              <Catalog24/>
              <div class="large-title">
                Explore previously scanned CBOMs
              </div>
            </div>
            <ListTable />
        </div>

        <div style="display: flex;">
          <cv-tile kind="standard" style="margin-right: 1.5%; flex: 7">
            <div class="card-container">
              <div class="icon-and-title-container">
                <AddAlt24/>
                <div class="title">
                  Generate a new CBOM
                </div>
              </div>
              <p class="body-text">
                Submit a new public Git repository to scan and generate a CBOM.
              </p>
              <SearchBar/>
            </div>
          </cv-tile>

          <cv-tile kind="standard" style="margin-left: 1.5%; flex: 4">
            <div class="card-container">
              <div class="icon-and-title-container">
                <Script24/>
                <div class="title">
                  Upload a CBOM
                </div>
              </div>
              <p class="body-text">
                Upload an existing CBOM to visualize it.
              </p>
              <FileUploader/>
            </div>
          </cv-tile>
        </div>
    </div>
</template>

<script>
import { model } from "@/model.js";
import FileUploader from "@/components/home/FileUploader.vue";
import SearchBar from "@/components/home/SearchBar.vue";
import ListTable from "@/components/home/ListTable.vue";
import { AddAlt24, Script24, Catalog24 } from "@carbon/icons-vue";


export default {
  name: "SearchOrUploadView",
  components: {
    SearchBar,
    ListTable,
    FileUploader,
    AddAlt24,
    Script24,
    Catalog24
  },
  data() {
    return {
      model,
    };
  },
}
</script>

<style scoped>
.list-table {
  padding: 2% 0% 4%;
  margin: auto;
}
.card-container {
  padding: 8px;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.icon-and-title-container {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
.title {
  font-size: large;
  font-weight: 500;
  padding-left: 8px;
}
.large-title {
  font-size: large;
  font-weight: 500;
  padding-left: 8px;
}
.small-header {
  font-size: small;
  font-weight: 600;
  margin-bottom: 10px;
}
.body-text {
  padding-bottom: 16px;
}
</style>
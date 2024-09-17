#!/bin/sh

ROOT_DIR=/opt/app

echo "Replacing env constants in JS"
for file in $ROOT_DIR/js/app.*.js* $ROOT_DIR/index.html;
do
  echo "Processing $file ...";

  sed -i 's|VUE_APP_HTTP_API_BASE_value|'${VUE_APP_HTTP_API_BASE}'|g' $file
  sed -i 's|VUE_APP_WS_API_BASE_value|'${VUE_APP_WS_API_BASE}'|g' $file
  sed -i 's|VUE_APP_TITLE_value|'${VUE_APP_TITLE}'|g' $file
  sed -i 's|VUE_APP_VIEWER_ONLY_value|'${VUE_APP_VIEWER_ONLY}'|g' $file

  echo "result $?"

done

echo "Starting Nginx"
nginx -g 'daemon off;'
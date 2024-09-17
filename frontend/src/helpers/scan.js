import { model, ErrorStatus } from "@/model.js";
import { setCbom } from "@/helpers.js";
import uuid4 from "uuid4";
import { STATES } from "@carbon/vue/src/components/cv-inline-loading";
import { API_SCAN_URL } from "@/app.config";

// This var is set to true when the client closes the socket. It is reinitialized to false when a new socket is created.
var socketWasManuallyClosed = false;

function startWebSocket(socketURL) {
  // Check if WebSocket is already open
  if (
    model.scanning.socket &&
    model.scanning.socket.readyState === WebSocket.OPEN
  ) {
    console.error("WebSocket is already open.");
    model.addError(null); // Display 'Unknown error'
    return;
  }

  // Create a new WebSocket instance
  model.scanning.socket = new WebSocket(socketURL);
  // Reset manual closing boolean tracker
  socketWasManuallyClosed = false;

  // WebSocket event listeners
  model.scanning.socket.addEventListener("open", () => {
    console.log("WebSocket connection opened.");
    scan();
  });

  model.scanning.socket.addEventListener("message", (event) => {
    // console.log('Received message:', event.data);
    let message = String(event.data.trim());
    handleMessage(message);
  });

  model.scanning.socket.addEventListener("close", (event) => {
    console.log("WebSocket connection closed.", event);
  });

  model.scanning.socket.addEventListener("error", (error) => {
    if (socketWasManuallyClosed) {
      // In safari, manually closing the connection creates an error:
      // Do not display an error when the connection is manually closed by the user
      console.warn(
        "The connection was closed by the client. An connection error occured, but has NOT been notified in the UI."
      );
    } else {
      console.error("WebSocket error:", error);
      model.addError(ErrorStatus.NoConnection);
    }
  });
}

export function stopWebSocket() {
  // Check if WebSocket is not null and is open
  if (
    model.scanning.socket &&
    model.scanning.socket.readyState === WebSocket.OPEN
  ) {
    socketWasManuallyClosed = true;
    model.scanning.socket.close(); // Close the WebSocket connection
    console.log("The client asked to close the WebSocket");
  }
  // else {
  //     console.warn('WebSocket is not open.');
  // }
}

export function connectAndScan(gitBranch, gitSubfolder) {
  model.resetScanningInfo();
  setAndCleanCodeOrigin(gitBranch, gitSubfolder);
  let clientId = uuid4();
  let socketURL = `${API_SCAN_URL}/${clientId}`;
  startWebSocket(socketURL);
}

function scan() {
  if (!model.scanning.socket) {
    model.addError(ErrorStatus.NoConnection);
    console.log("No socket in model");
  } else if (!model.codeOrigin.gitLink) {
    // TODO: Should I validate the look of the Git link in the frontend?
    model.addError(ErrorStatus.InvalidRepo);
    console.log("Git URL not valid");
  } else {
    var request = {};
    request["gitUrl"] = model.codeOrigin.gitLink;
    if (model.codeOrigin.gitBranch) {
      request["branch"] = model.codeOrigin.gitBranch;
    }
    if (model.codeOrigin.gitSubfolder) {
      request["subfolder"] = model.codeOrigin.gitSubfolder;
    }
    model.scanning.socket.send(JSON.stringify(request));
    // this.filterOpen = false
    model.scanning.isScanning = true;
    model.scanning.scanningStatus = STATES.LOADING;
    model.showResults = true;
  }
}

function handleMessage(messageJson) {
  let obj = JSON.parse(messageJson);
  // console.log(obj)
  if (obj["type"] === "LABEL") {
    model.scanning.scanningStatusMessage = obj["message"];
    if (obj["message"] === "Starting...") {
      model.scanning.startTime = new Date();
    }
    if (obj["message"] === "Finished") {
      model.scanning.scanningStatus = STATES.LOADED;
      model.scanning.isScanning = false;
      let finishTime = new Date();
      model.scanning.totalDuration = Math.floor(
        (finishTime - model.scanning.startTime) / 1000
      ); // Time in seconds
    }
  } else if (obj["type"] === "ERROR") {
    model.addError(ErrorStatus.BranchNotSpecified); // TODO: When several different error messages will exist, this will have to be changed
    model.scanning.scanningStatusMessage = obj["message"];
    console.error("Error from backend:", model.scanning.scanningStatusMessage);
    model.scanning.scanningStatus = STATES.ERROR;
    model.scanning.isScanning = false;
  } else if (obj["type"] === "PURL") {
    model.codeOrigin.gitPurls = obj["purls"];
    // This is not strictly necessary anymore now that I read PURLs from the CBOM, but it arrives before the CBOM so I leave it
  } else if (obj["type"] === "DETECTION") {
    let cryptoAssetJson = obj["message"];
    const cryptoAsset = JSON.parse(cryptoAssetJson);
    model.scanning.liveDetections.push(cryptoAsset);
    // console.log("New detection:",obj)
  } else if (obj["type"] === "CBOM") {
    let cbomString = obj["message"];
    setCbom(JSON.parse(cbomString));
    console.log("Received CBOM from scanning:", model.cbom);
  } else if (obj["type"] === "BRANCH") {
    model.codeOrigin.gitBranch = obj["message"];
  } else if (obj["type"] === "SCANNED_FILE_COUNT") {
    model.scanning.numberOfFiles = obj["message"];
  } else if (obj["type"] === "SCANNED_NUMBER_OF_LINES") {
    model.scanning.numberOfLines = obj["message"];
  } else if (obj["type"] === "SCANNED_DURATION") {
    model.scanning.scanDuration = obj["message"];
  } else if (obj["type"] === "REVISION_HASH") {
    model.codeOrigin.commitID = obj["message"];
  } else {
    console.log("Unknown message:", messageJson);
  }
}

function setAndCleanCodeOrigin(gitBranch, gitSubfolder) {
  if (model.codeOrigin.gitLink) {
    model.codeOrigin.gitLink = model.codeOrigin.gitLink.trim();
  }
  if (gitBranch) {
    model.codeOrigin.gitBranch = gitBranch.trim();
  }
  if (gitSubfolder) {
    model.codeOrigin.gitSubfolder = gitSubfolder.trim();
  }
}

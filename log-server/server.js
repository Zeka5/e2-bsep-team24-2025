const express = require("express");
const fs = require("fs");
const path = require("path");

const app = express();
const PORT = 3001;

app.use(express.json());

const logsDir = path.join(__dirname, "logs");
if (!fs.existsSync(logsDir)) {
  fs.mkdirSync(logsDir);
}

app.post("/api/logs", (req, res) => {
  try {
    const { timestamp, level, logger, message, thread } = req.body;

    const logEntry = `${timestamp} [${thread}] ${level} ${logger} - ${message}\n`;

    // UTC+2 timezone
    const now = new Date();
    const utcPlus2 = new Date(now.getTime() + 2 * 60 * 60 * 1000);
    const today = utcPlus2.toISOString().split("T")[0];
    const logFileName = `app-${today}.log`;
    const logFilePath = path.join(logsDir, logFileName);

    fs.appendFileSync(logFilePath, logEntry, "utf8");

    console.log(`Log received: ${level} - ${message}`);
    res.status(200).json({ status: "success", message: "Log saved" });
  } catch (error) {
    console.error("Error saving log:", error);
    res.status(500).json({ status: "error", message: "Failed to save log" });
  }
});

app.listen(PORT, () => {
  console.log(`Log server running on http://localhost:${PORT}`);
  console.log(`Logs will be saved to: ${logsDir}`);
});

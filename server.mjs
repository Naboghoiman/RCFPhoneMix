import express from "express";
import OpenAI from "openai";

const app = express();
app.use(express.json({ limit: "1mb" }));

const client = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });
const APP_TOKEN = process.env.IMAN_APP_TOKEN || "";

const ACTIONS = [
  "answer","play_song","pause_music","resume_music","stop_music",
  "call_contact","open_app","set_alarm","flashlight_on","flashlight_off",
  "volume_up","volume_down","take_photo","web_search"
];

const schema = {
  type: "object",
  additionalProperties: false,
  properties: {
    action: { type: "string", enum: ACTIONS },
    argument: { type: "string" },
    response: { type: "string" }
  },
  required: ["action","argument","response"]
};

app.get("/", (_req, res) => res.json({
  ok: true,
  name: "IMAN AI PHONE ROBOT",
  version: "1.0"
}));

app.post("/agent", async (req, res) => {
  try {
    if (APP_TOKEN && req.get("x-iman-token") !== APP_TOKEN) {
      return res.status(401).json({ error: "Unauthorized" });
    }

    const text = String(req.body?.text || "").trim();
    const memory = String(req.body?.memory || "").slice(-8000);
    if (!text) return res.status(400).json({ error: "Missing text" });

    const instructions = `
You are IMAN AI PHONE ROBOT, a concise Android voice agent.
Choose exactly one phone action for the user's request.
Use "answer" for ordinary conversation or knowledge.
For play_song: argument is local song title/artist search text.
For call_contact: argument is the saved contact name.
For open_app: argument is installed app display name.
For set_alarm: argument MUST be local 24-hour HH:mm.
For web_search: argument is the search query.
For no-argument actions, argument is "".
response is a short natural sentence suitable for text-to-speech.
Never claim a phone action succeeded; the Android app reports execution.
Recent memory:
${memory}
`;

    const response = await client.responses.create({
      model: "gpt-5",
      instructions,
      input: text,
      text: {
        format: {
          type: "json_schema",
          name: "phone_action",
          strict: true,
          schema
        }
      }
    });

    const parsed = JSON.parse(response.output_text);
    res.json(parsed);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err?.message || "Server error" });
  }
});

const port = process.env.PORT || 3000;
app.listen(port, () => console.log(`IMAN server listening on ${port}`));

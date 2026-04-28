import google.generativeai as genai
from config import GEMINI_API_KEY
from services.vector_store import vector_store
from typing import List, Dict, Any
import json
import time

genai.configure(api_key=GEMINI_API_KEY)

AGENT_PROMPT = """You are SmartDesk AI Agent. Break down complex queries.

Available tools:
1. knowledge_search(query) - Search the knowledge base
2. summarize(text) - Summarize provided text
3. compare(items) - Compare multiple items

Given query: {query}

Previous steps: {steps}

Respond in JSON:
{{
  "thought": "reasoning here",
  "action": "tool_name or FINAL_ANSWER",
  "action_input": "input for tool or final answer",
  "done": true/false
}}"""


class AgentService:
    def __init__(self):
        self.model = genai.GenerativeModel("gemini-1.5-flash")
        self.max_steps = 5

    def run(self, query: str,
            history: List[Dict[str, str]]) -> Dict[str, Any]:
        steps = []
        tools_used = []
        start = time.time()

        for step_num in range(self.max_steps):
            steps_summary = json.dumps(steps[-3:]) if steps else "[]"
            prompt = AGENT_PROMPT.format(
                query=query, steps=steps_summary)

            try:
                response = self.model.generate_content(prompt)
                text = response.text.strip()
                if text.startswith("```"):
                    text = text.split("```")[1]
                    if text.startswith("json"):
                        text = text[4:]
                parsed = json.loads(text)
            except Exception as e:
                parsed = {
                    "thought": "Error parsing response",
                    "action": "FINAL_ANSWER",
                    "action_input": f"Error: {str(e)}",
                    "done": True
                }

            step = {
                "step": step_num + 1,
                "thought": parsed.get("thought", ""),
                "action": parsed.get("action", ""),
                "action_input": parsed.get("action_input", ""),
                "result": ""
            }

            action = parsed.get("action", "FINAL_ANSWER")

            if action == "knowledge_search":
                results = vector_store.search(
                    parsed.get("action_input", query), top_k=3)
                step["result"] = "\n".join(
                    [r["content"][:200] for r in results])
                tools_used.append("knowledge_search")

            elif action == "summarize":
                step["result"] = parsed.get("action_input", "")[:500]
                tools_used.append("summarize")

            elif action == "FINAL_ANSWER" or parsed.get("done", False):
                steps.append(step)
                return {
                    "answer": parsed.get("action_input", "No answer"),
                    "steps": steps,
                    "tools_used": list(set(tools_used)),
                    "latency_ms": int((time.time() - start) * 1000)
                }

            steps.append(step)

        return {
            "answer": steps[-1].get("action_input",
                                    "Could not complete query"),
            "steps": steps,
            "tools_used": list(set(tools_used)),
            "latency_ms": int((time.time() - start) * 1000)
        }

agent_service = AgentService()
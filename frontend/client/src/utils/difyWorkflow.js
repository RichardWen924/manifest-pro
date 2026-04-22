export function normalizeDifyWorkflowMappings(input) {
  const payload = parseDifyWorkflowPayload(input);
  const mappings = findMappings(payload);

  if (!Array.isArray(mappings)) {
    return [];
  }

  return mappings
    .filter((item) => item && typeof item === "object")
    .map((item) => ({
      originalText: item.original_text ?? item.originalText ?? "",
      placeholderKey: item.placeholder_key ?? item.placeholderKey ?? "",
      dataType: item.data_type ?? item.dataType ?? "string",
      description: item.description ?? "",
      raw: item,
    }))
    .filter((item) => item.placeholderKey);
}

export function parseDifyWorkflowPayload(input) {
  if (!input) {
    return null;
  }

  if (typeof input === "object") {
    if (typeof input.text === "string") {
      return parseDifyWorkflowPayload(input.text);
    }
    if (typeof input.answer === "string") {
      return parseDifyWorkflowPayload(input.answer);
    }
    return input;
  }

  if (typeof input !== "string") {
    return null;
  }

  const cleaned = stripMarkdownJsonFence(input.trim());
  try {
    const parsed = JSON.parse(cleaned);
    return parseDifyWorkflowPayload(parsed);
  } catch (error) {
    return null;
  }
}

function stripMarkdownJsonFence(value) {
  const fenceMatch = value.match(/^```(?:json)?\s*([\s\S]*?)\s*```$/i);
  return fenceMatch ? fenceMatch[1].trim() : value;
}

function findMappings(payload) {
  if (!payload || typeof payload !== "object") {
    return [];
  }

  if (Array.isArray(payload.mappings)) {
    return payload.mappings;
  }

  if (Array.isArray(payload.data?.mappings)) {
    return payload.data.mappings;
  }

  if (Array.isArray(payload.result?.mappings)) {
    return payload.result.mappings;
  }

  if (Array.isArray(payload.outputs?.mappings)) {
    return payload.outputs.mappings;
  }

  return [];
}

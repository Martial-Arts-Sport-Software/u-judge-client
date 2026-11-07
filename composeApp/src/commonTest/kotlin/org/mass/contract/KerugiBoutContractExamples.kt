package org.mass.contract

/**
 * The messages of server ADR-009 (`docs/adr/ADR-009-kerugi-bout-contract.md`, u-judge-server d1ce2b4), copied verbatim.
 * The server adds them as contract fixtures with its I2a implementation; once `contract/server-ref` points at that
 * commit these tests read [ServerContractFixtures] instead.
 */
object KerugiBoutContractExamples {
    val sessionSnapshot = """
        {
          "type": "session_snapshot",
          "cursor": 118,
          "session": {
            "sessionId": "9c8b7a6d-5e4f-4d3c-8b2a-1f0e9d8c7b60",
            "discipline": "kerugi",
            "competitionId": "3f6c1d2e-8a4b-4c7d-9e1f-0a2b3c4d5e60",
            "peerId": "00000000-0000-4000-8000-000000000001",
            "courtId": "5b7e2c10-1d3f-4a6b-8c9d-0e1f2a3b4c50",
            "courtNumber": 1,
            "bracketId": "7a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c40",
            "categoryTitle": "Юноши 12-14 лет, 45 кг",
            "boutNumber": 7,
            "state": "running",
            "blue": {
              "entryId": "entry-12",
              "label": "Иванов И. И. (Москва)",
              "athletes": [{ "fullName": "Иванов Иван Иванович", "city": "Москва" }]
            },
            "red": {
              "entryId": "entry-31",
              "label": "Петров П. П. (Казань)",
              "athletes": [{ "fullName": "Петров Пётр Петрович", "city": "Казань" }]
            },
            "settings": { "judgeCount": 3, "quorum": 2, "coincidenceWindowMillis": 1000 },
            "seat": {
              "number": 2,
              "judgeId": "2d4f6a8c-0e1b-4c3d-9e5f-7a9b1c3d5e70",
              "deviceId": "android-contract"
            },
            "score": {
              "blue": 3,
              "red": 1,
              "gamjeom": { "blue": 0, "red": 1 },
              "warnings": [],
              "disqualified": null
            },
            "result": null
          }
        }
    """.trimIndent()

    const val SESSION_SNAPSHOT_IDLE = """{ "type": "session_snapshot", "cursor": 57, "session": null }"""

    const val RESYNC_REQUEST =
        """{ "type": "resync_request", "requestId": "1e2d3c4b-5a69-4788-9a0b-c1d2e3f4a5b6", "cursor": 112 }"""

    /** The ADR abbreviates the session (`"...": "as in section 3"`); it is the session of [sessionSnapshot]. */
    val resyncResponse = sessionSnapshot
        .replace("\"type\": \"session_snapshot\"", "\"type\": \"resync_response\", \"requestId\": \"1e2d3c4b-5a69-4788-9a0b-c1d2e3f4a5b6\"")

    const val RESYNC_RESPONSE_IDLE =
        """{ "type": "resync_response", "requestId": "1e2d3c4b-5a69-4788-9a0b-c1d2e3f4a5b6", "cursor": 57, "session": null }"""

    const val RESYNC_REJECTED =
        """{ "type": "resync_rejected", "requestId": "1e2d3c4b-5a69-4788-9a0b-c1d2e3f4a5b6", "code": "journal_unavailable" }"""

    val sessionStateUpdated = """
        {
          "type": "session_state_updated",
          "sessionId": "9c8b7a6d-5e4f-4d3c-8b2a-1f0e9d8c7b60",
          "cursor": 119,
          "state": "completed",
          "result": { "winner": "BLUE", "reason": "final_score" }
        }
    """.trimIndent()

    val kerugiScoreCommand = """
        {
          "type": "kerugi_score_command",
          "eventId": "6f1c8a52-3c1e-4c55-9a3e-2b9f0d6e4a10",
          "sequence": 0,
          "clientTimestamp": "2026-11-08T10:15:30.120Z",
          "competitionId": "3f6c1d2e-8a4b-4c7d-9e1f-0a2b3c4d5e60",
          "peerId": "00000000-0000-4000-8000-000000000001",
          "courtId": "5b7e2c10-1d3f-4a6b-8c9d-0e1f2a3b4c50",
          "bracketId": "7a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c40",
          "sessionId": "9c8b7a6d-5e4f-4d3c-8b2a-1f0e9d8c7b60",
          "judgeId": "2d4f6a8c-0e1b-4c3d-9e5f-7a9b1c3d5e70",
          "deviceId": "android-contract",
          "competitor": "BLUE",
          "area": "HEAD"
        }
    """.trimIndent()

    val judgeAttentionCommand = """
        {
          "type": "judge_attention_command",
          "eventId": "8b3e5d7f-1a2c-4e4b-9d6f-3c5e7a9b1d20",
          "sequence": 1,
          "clientTimestamp": "2026-11-08T10:15:41.500Z",
          "competitionId": "3f6c1d2e-8a4b-4c7d-9e1f-0a2b3c4d5e60",
          "peerId": "00000000-0000-4000-8000-000000000001",
          "courtId": "5b7e2c10-1d3f-4a6b-8c9d-0e1f2a3b4c50",
          "bracketId": "7a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c40",
          "sessionId": "9c8b7a6d-5e4f-4d3c-8b2a-1f0e9d8c7b60",
          "judgeId": "2d4f6a8c-0e1b-4c3d-9e5f-7a9b1c3d5e70",
          "deviceId": "android-contract"
        }
    """.trimIndent()

    const val COMMAND_ACK = """{ "type": "command_ack", "eventId": "6f1c8a52-3c1e-4c55-9a3e-2b9f0d6e4a10" }"""

    const val COMMAND_REJECTED =
        """{ "type": "command_rejected", "code": "session_closed", "eventId": "6f1c8a52-3c1e-4c55-9a3e-2b9f0d6e4a10" }"""

    val kerugiScoreUpdated = """
        {
          "type": "kerugi_score_updated",
          "sessionId": "9c8b7a6d-5e4f-4d3c-8b2a-1f0e9d8c7b60",
          "cursor": 124,
          "score": {
            "blue": 5,
            "red": 1,
            "gamjeom": { "blue": 0, "red": 10 },
            "warnings": [{ "code": "disqualification_pending", "competitor": "RED", "gamjeomCount": 10 }],
            "disqualified": null
          }
        }
    """.trimIndent()
}

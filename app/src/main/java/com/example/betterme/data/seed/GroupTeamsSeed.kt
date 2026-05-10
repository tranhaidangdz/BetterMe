package com.example.betterme.data.seed

import com.example.betterme.data.local.room.entities.GroupTeamEntity

/**
 * Seeded teams for group challenges (16 + 17). Three teams per group, ranked by total_coins.
 */
object GroupTeamsSeed {

    val teams: List<GroupTeamEntity> = listOf(
        // Challenge 16 - Cùng nhau khoẻ mạnh
        GroupTeamEntity(
            id = 1,
            challenge_id = 16,
            name = "Đội Cá Heo",
            icon_emoji = "🐬",
            color_hex = "#0EA5E9",
            member_count = 420,
            total_coins = 1250,
            rank = 1
        ),
        GroupTeamEntity(
            id = 2,
            challenge_id = 16,
            name = "Đội Gấu Trúc",
            icon_emoji = "🐼",
            color_hex = "#64748B",
            member_count = 380,
            total_coins = 1100,
            rank = 2
        ),
        GroupTeamEntity(
            id = 3,
            challenge_id = 16,
            name = "Đội Sư Tử",
            icon_emoji = "🦁",
            color_hex = "#F59E0B",
            member_count = 447,
            total_coins = 950,
            rank = 3
        ),

        // Challenge 17 - Đọc sách cùng nhau
        GroupTeamEntity(
            id = 4,
            challenge_id = 17,
            name = "Đội Mọt Sách",
            icon_emoji = "📚",
            color_hex = "#8B5CF6",
            member_count = 280,
            total_coins = 720,
            rank = 1
        ),
        GroupTeamEntity(
            id = 5,
            challenge_id = 17,
            name = "Đội Bookworm",
            icon_emoji = "🐛",
            color_hex = "#10B981",
            member_count = 263,
            total_coins = 640,
            rank = 2
        ),
        GroupTeamEntity(
            id = 6,
            challenge_id = 17,
            name = "Đội Cú Mèo",
            icon_emoji = "🦉",
            color_hex = "#7C3AED",
            member_count = 280,
            total_coins = 560,
            rank = 3
        )
    )
}

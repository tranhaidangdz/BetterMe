package com.example.betterme.data.seed

import com.example.betterme.data.local.room.entities.ChallengeEntity

/**
 * Prestige-tier catalog expansion — 15 challenges across three
 * progressively harder bands. Sits alongside [ChallengesSeed],
 * [StarterChallengesSeed], [EliteChallengesSeed] and
 * [UpcomingChallengesSeed]; flows through the same Discover → Detail →
 * Join → check-in pipeline as every other catalog source.
 *
 * ### IDs
 * `400..414` — chosen to avoid every existing seed range:
 *  - ChallengesSeed: 1..48
 *  - UpcomingChallengesSeed: 100..119
 *  - EliteChallengesSeed: 200..219
 *  - StarterChallengesSeed: 300..319
 *
 * ### Tier distribution
 *  - **MEDIUM** (5 · IDs 400..404) — 7-14 days, 300-800 coins.
 *  - **HARD** (5 · IDs 405..409) — 21-45 days, 1000-3000 coins.
 *  - **LEGENDARY** (5 · IDs 410..414) — 60-120 days, 5000-15000 coins.
 *
 * ### Theme coverage
 * Hydration · mindfulness · finance · dopamine detox · reading ·
 * fitness · discipline · sleep · deep work · digital detox.
 *
 * ### Why this lives in a separate seed file
 * The existing `ChallengesSeed` is a 48-entry static catalog that
 * was hand-tuned at app launch; touching it risks rewriting
 * already-shipped content. This file extends the catalog purely by
 * INSERTing new IDs — the seeder uses `OnConflictStrategy.IGNORE` so
 * re-seeding on upgrade is safe.
 *
 * Reward badges reuse the [BadgesSeed] pool (IDs 1..30) — no schema
 * change, no new badge assets.
 */
object PrestigeChallengesSeed {

    fun prestige(now: Long = System.currentTimeMillis()): List<ChallengeEntity> = listOf(
        // ==============================================================
        // MEDIUM TIER  (IDs 400..404)  ·  7-14 days  ·  300-800 coins
        // ==============================================================
        ChallengeEntity(
            id = 400, title = "Hydration Pro 10 ngày",
            description = "10 ngày uống đủ 2 lít nước mỗi ngày — chia đều thành 8 ly. Theo dõi bằng đồng hồ uống nước hoặc bằng chiếc bình nước có vạch chia. Cơ thể đủ nước thì tinh thần và năng lượng cũng đủ.",
            short_description = "2 lít nước × 10 ngày",
            motivational_quote = "Nước là nền tảng — mọi sự tập trung khác đều khó xảy ra khi cơ thể đang thiếu.",
            completion_message = "10 ngày uống đủ nước — bạn đã đặt nền cho mọi thói quen sức khoẻ khác.",
            category_id = 2, difficulty = "MEDIUM",
            duration_days = 10, target_streak = 10, reward_coins = 300, reward_badge_id = 2,
            icon_emoji = "💧", color_hex = "#0EA5E9",
            participant_count = 8400, sort_order = 400, created_at = now
        ),
        ChallengeEntity(
            id = 401, title = "Buổi sáng chánh niệm 14 ngày",
            description = "Mỗi sáng dành 10 phút thiền hoặc thở sâu trước khi chạm điện thoại. Trong 14 ngày, bạn sẽ nhận ra một sự khác biệt rõ rệt về sự bình thản và tập trung suốt cả ngày.",
            short_description = "10 phút thiền sáng × 14 ngày",
            motivational_quote = "10 phút buổi sáng định hình 24 giờ còn lại.",
            completion_message = "Hai tuần buổi sáng chánh niệm — bạn đã rèn được một liều thuốc tinh thần.",
            category_id = 3, difficulty = "MEDIUM",
            duration_days = 14, target_streak = 14, reward_coins = 450, reward_badge_id = 14,
            icon_emoji = "🧘", color_hex = "#8B5CF6", is_featured = true,
            participant_count = 6700, sort_order = 401, created_at = now
        ),
        ChallengeEntity(
            id = 402, title = "Đọc 30 phút mỗi ngày",
            description = "14 ngày đọc 30 phút mỗi ngày — không đọc lướt, không đọc nhanh, đọc để hiểu. Tích lũy 7 giờ đọc sâu, có thể là một cuốn sách phi hư cấu trung bình hoặc một nửa cuốn dày.",
            short_description = "30 phút đọc × 14 ngày",
            motivational_quote = "Người đọc sách nhiều không nhất thiết giàu hơn — nhưng họ có nhiều lựa chọn hơn.",
            completion_message = "7 giờ đọc sâu — đó là khoản đầu tư cho bộ não bạn vừa hoàn tất.",
            category_id = 4, difficulty = "MEDIUM",
            duration_days = 14, target_streak = 14, reward_coins = 500, reward_badge_id = 13,
            icon_emoji = "📚", color_hex = "#0EA5E9",
            participant_count = 7200, sort_order = 402, created_at = now
        ),
        ChallengeEntity(
            id = 403, title = "Ghi chi tiêu 2 tuần",
            description = "14 ngày ghi mọi khoản chi — từ cốc cà phê đến hoá đơn lớn. Mục tiêu không phải tiết kiệm mà là nhận thức. Cuối tuần thứ hai, bạn sẽ thấy bức tranh tài chính của mình rõ hơn bất kỳ app ngân hàng nào.",
            short_description = "Ghi chi tiêu × 14 ngày",
            motivational_quote = "Bạn không thể quản lý cái mà bạn không đo lường.",
            completion_message = "14 ngày minh bạch tài chính — bạn vừa mở mắt khỏi một sương mù vô hình.",
            category_id = 5, difficulty = "MEDIUM",
            duration_days = 14, target_streak = 14, reward_coins = 400, reward_badge_id = 22,
            icon_emoji = "💰", color_hex = "#F59E0B",
            participant_count = 5300, sort_order = 403, created_at = now
        ),
        ChallengeEntity(
            id = 404, title = "Dopamine Detox Lite",
            description = "10 ngày không lướt mạng xã hội ngoài giờ làm việc — đặt giới hạn 30 phút/ngày tổng cộng. Thay vào đó đi bộ, đọc sách, gọi điện cho người thân. Bộ não bạn sẽ nhớ ra niềm vui không cần thuật toán.",
            short_description = "Giới hạn 30 phút mạng XH × 10 ngày",
            motivational_quote = "Sự nhàm chán là nơi sáng tạo bắt đầu — không phải ở scroll vô tận.",
            completion_message = "Bạn vừa giành lại 20+ giờ và một bộ não bình thản hơn.",
            category_id = 5, difficulty = "MEDIUM",
            duration_days = 10, target_streak = 10, reward_coins = 700, reward_badge_id = 23,
            icon_emoji = "📵", color_hex = "#6366F1", is_featured = true,
            participant_count = 4800, sort_order = 404, created_at = now
        ),

        // ==============================================================
        // HARD TIER  (IDs 405..409)  ·  21-45 days  ·  1000-3000 coins
        // ==============================================================
        ChallengeEntity(
            id = 405, title = "30 ngày không đường tinh chế",
            description = "30 ngày loại bỏ đường tinh chế khỏi chế độ ăn — không nước ngọt, không bánh kẹo, không soda. Trái cây tự nhiên vẫn được. Đây là phép thử thẳng thắn về kỷ luật ăn uống.",
            short_description = "Không đường × 30 ngày",
            motivational_quote = "Đường là kẻ giết người chậm rãi — và bạn vừa cắt đứt nó.",
            completion_message = "30 ngày không đường — vị giác, năng lượng và làn da của bạn đã được hồi sinh.",
            category_id = 2, difficulty = "HARD",
            duration_days = 30, target_streak = 30, reward_coins = 1200, reward_badge_id = 5,
            icon_emoji = "🚫🍬", color_hex = "#DC2626",
            participant_count = 3400, sort_order = 405, created_at = now
        ),
        ChallengeEntity(
            id = 406, title = "Dậy trước 6:00 trong 21 ngày",
            description = "21 ngày liên tiếp dậy trước 6:00 sáng — bất kể cuối tuần, mưa, hay đêm trước đó muộn. Đây là một trong những thử thách định hình kỷ luật mạnh nhất.",
            short_description = "Dậy trước 6:00 × 21 ngày",
            motivational_quote = "Buổi sáng là khoảng thời gian duy nhất trong ngày bạn được sống cho chính mình.",
            completion_message = "21 buổi sáng yên tĩnh — bạn đã thắng cuộc chiến với chăn gối.",
            category_id = 5, difficulty = "HARD",
            duration_days = 21, target_streak = 21, reward_coins = 1500, reward_badge_id = 21,
            icon_emoji = "🌅", color_hex = "#F97316", is_featured = true,
            participant_count = 4100, sort_order = 406, created_at = now
        ),
        ChallengeEntity(
            id = 407, title = "Deep Work 45 ngày",
            description = "45 ngày deep work — mỗi ngày 2 phiên 60 phút không gián đoạn, không điện thoại, không nhắn tin, không tab tin tức. Một trong những phép thử khắc nghiệt nhất với khả năng tập trung của bạn.",
            short_description = "2×60 phút deep work × 45 ngày",
            motivational_quote = "Tập trung sâu là siêu năng lực của thế kỷ này — và bạn đang xây dựng nó.",
            completion_message = "45 ngày deep work — bạn đã trở thành 1% người làm việc thật sự sâu.",
            category_id = 4, difficulty = "HARD",
            duration_days = 45, target_streak = 45, reward_coins = 2800, reward_badge_id = 24,
            icon_emoji = "🎯", color_hex = "#EA580C",
            participant_count = 2300, sort_order = 407, created_at = now
        ),
        ChallengeEntity(
            id = 408, title = "Cardio 30 phút mỗi ngày",
            description = "30 ngày cardio mỗi ngày — chạy bộ, đạp xe, bơi, hoặc nhảy dây. Không có ngày nghỉ. Cơ thể bạn sẽ phản đối tuần đầu, biết ơn tuần thứ hai, và thay đổi rõ rệt tuần thứ ba.",
            short_description = "Cardio 30 phút × 30 ngày",
            motivational_quote = "Đau hôm nay là sức mạnh ngày mai.",
            completion_message = "30 ngày cardio — tim bạn khỏe hơn, ngực bạn rộng hơn, và bạn biết mình làm được.",
            category_id = 1, difficulty = "HARD",
            duration_days = 30, target_streak = 30, reward_coins = 1800, reward_badge_id = 7,
            icon_emoji = "🏃‍♂️", color_hex = "#DC2626",
            participant_count = 3700, sort_order = 408, created_at = now
        ),
        ChallengeEntity(
            id = 409, title = "Kỷ luật 30 ngày toàn diện",
            description = "30 ngày giữ đúng 5 cam kết mỗi ngày: dậy đúng giờ, tập 30 phút, đọc 30 phút, không mạng xã hội ngoài giờ, ngủ trước 23:00. Đây là phép thử kỷ luật tổng hợp.",
            short_description = "5 cam kết × 30 ngày",
            motivational_quote = "Kỷ luật không phải sự trừng phạt — nó là sự tự do.",
            completion_message = "30 ngày kỷ luật — bạn đã trở thành phiên bản mạnh hơn của chính mình.",
            category_id = 5, difficulty = "HARD",
            duration_days = 30, target_streak = 30, reward_coins = 2400, reward_badge_id = 25,
            icon_emoji = "⚔️", color_hex = "#7C2D12",
            participant_count = 2800, sort_order = 409, created_at = now
        ),

        // ==============================================================
        // LEGENDARY TIER  (IDs 410..414)  ·  60-120 days  ·  5000-15000 coins
        // ==============================================================
        ChallengeEntity(
            id = 410, title = "90 ngày không đường — Bậc Thầy",
            description = "90 ngày không một hạt đường tinh chế. Không nước ngọt, không bánh kẹo, không cà phê có đường, không salad có sốt chứa đường. Chỉ trái cây tự nhiên. Đây là bài kiểm tra kỷ luật ăn uống ở mức cao nhất.",
            short_description = "Zero đường × 90 ngày",
            motivational_quote = "90 ngày — đủ để vị giác, đường huyết và cách bạn nhìn thức ăn thay đổi mãi mãi.",
            completion_message = "90 ngày — bạn đã viết lại mối quan hệ với đồ ăn. Đây là một trong những thành tựu hiếm nhất.",
            category_id = 2, difficulty = "LEGENDARY",
            duration_days = 90, target_streak = 90, reward_coins = 8000, reward_badge_id = 5,
            icon_emoji = "🏆", color_hex = "#7C3AED", is_featured = true,
            participant_count = 1100, sort_order = 410, created_at = now
        ),
        ChallengeEntity(
            id = 411, title = "100 ngày Deep Work",
            description = "100 ngày deep work liên tục — 2 phiên 90 phút mỗi ngày. Không điện thoại, không Slack, không tab tin tức. Đây là cấp độ tu luyện mà <1% người làm việc tri thức từng chạm tới.",
            short_description = "2×90 phút deep work × 100 ngày",
            motivational_quote = "100 ngày deep work là một sự nghiệp được nén lại — không nhiều người dám đi.",
            completion_message = "100 ngày deep work — bạn đã thay đổi quan hệ với sự tập trung mãi mãi. Đây là cấp Bậc Thầy thật sự.",
            category_id = 4, difficulty = "LEGENDARY",
            duration_days = 100, target_streak = 100, reward_coins = 12000, reward_badge_id = 24,
            icon_emoji = "🧠", color_hex = "#7C3AED", is_featured = true,
            participant_count = 720, sort_order = 411, created_at = now
        ),
        ChallengeEntity(
            id = 412, title = "75 Hard — BetterMe Edition",
            description = "75 ngày của địa ngục có kỷ luật. Mỗi ngày: 2 buổi tập 45 phút (1 ngoài trời), uống 4 lít nước, đọc 10 trang sách phi hư cấu, không rượu/đường tinh chế. Lỡ một ngày = khởi động lại từ ngày 1.",
            short_description = "75 ngày · 4 cam kết mỗi ngày · không thoả hiệp",
            motivational_quote = "75 ngày này không phải để dễ thở — nó là để rèn ra một con người khác.",
            completion_message = "Bạn vừa hoàn thành 75 Hard — bạn đã chứng minh với chính mình bạn có thể giữ lời.",
            category_id = 5, difficulty = "LEGENDARY",
            duration_days = 75, target_streak = 75, reward_coins = 10000, reward_badge_id = 25,
            icon_emoji = "🔥", color_hex = "#7C2D12", is_featured = true,
            participant_count = 850, sort_order = 412, created_at = now
        ),
        ChallengeEntity(
            id = 413, title = "Dậy trước 6:00 trong 60 ngày",
            description = "60 ngày liên tiếp dậy trước 6:00 sáng — bao gồm cả cuối tuần và ngày lễ. Đây là phép thử kỷ luật giấc ngủ ở cấp độ cao nhất. Hoàn thành nghĩa là bạn đã định hình lại đồng hồ sinh học của mình.",
            short_description = "Dậy trước 6:00 × 60 ngày",
            motivational_quote = "60 buổi sáng nguyên vẹn — đây là khoản đầu tư đẹp nhất bạn từng làm cho ngày của mình.",
            completion_message = "60 ngày dậy sớm — bạn đã sở hữu buổi sáng của mình theo cách rất ít người làm được.",
            category_id = 5, difficulty = "LEGENDARY",
            duration_days = 60, target_streak = 60, reward_coins = 6000, reward_badge_id = 21,
            icon_emoji = "🌄", color_hex = "#7C3AED",
            participant_count = 940, sort_order = 413, created_at = now
        ),
        ChallengeEntity(
            id = 414, title = "Digital Detox Master — 90 ngày",
            description = "90 ngày kỷ luật tuyệt đối với màn hình: không mạng xã hội cá nhân ngoài giờ làm, không tin tức buổi sáng, không điện thoại trong phòng ngủ, không màn hình sau 21:30. Sự bình thản trở về sau tuần đầu tiên.",
            short_description = "Zero mạng XH cá nhân × 90 ngày",
            motivational_quote = "Bộ não bạn đáng được nhiều hơn là một dòng tin vô tận. 90 ngày để chứng minh điều đó.",
            completion_message = "90 ngày detox — bạn vừa tái lập trình lại sự chú ý của mình. Đây là tài sản hiếm.",
            category_id = 3, difficulty = "LEGENDARY",
            duration_days = 90, target_streak = 90, reward_coins = 9000, reward_badge_id = 23,
            icon_emoji = "🧘‍♂️", color_hex = "#8B5CF6",
            participant_count = 1300, sort_order = 414, created_at = now
        )
    )
}

package com.example.betterme.data.seed

import com.example.betterme.data.local.room.entities.ChallengeEntity
import java.util.Calendar

/**
 * Production-grade HARD + LEGENDARY tier expansion.
 *
 * 20 entries — 10 HARD (IDs 200..209) and 10 LEGENDARY (IDs 210..219) — sharing the
 * same [ChallengeEntity] schema as [ChallengesSeed] and [UpcomingChallengesSeed]. There
 * is no separate "elite" runtime model: these flow through the exact same Discover /
 * Overview / Detail screens as every other challenge, get joined the same way, persist
 * progress in the same `user_challenges` + `challenge_logs` tables.
 *
 * A few entries set [ChallengeEntity.start_date] to a future timestamp at seed time so
 * the Discover "Sắp diễn ra" carousel and the Overview "Upcoming" filter pick them up
 * automatically. Once that date passes, the existing partition logic (`start_date <= now`)
 * promotes them to active without any code change.
 *
 * Reward badges 31..35 (HARD tier) and 36..40 (LEGENDARY tier) are seeded by
 * [BadgesSeed] in the same transaction.
 *
 * Category mapping (matching the existing [data.local.fake.fakeCategories] order):
 *   1 = Vận động & thể chất → Fitness
 *   2 = Dinh dưỡng           → Health
 *   3 = Tinh thần            → Mindfulness
 *   4 = Học tập              → Learning / Productivity
 *   5 = Kỷ luật              → Discipline
 *   6 = Mối quan hệ          → (intentionally unused at this tier)
 */
object EliteChallengesSeed {

    fun elite(now: Long = System.currentTimeMillis()): List<ChallengeEntity> {
        // Future-start helper. Seed time + N days; persistent in the DB so the
        // upcoming/active partition continues to work as time passes.
        fun inDays(n: Int): Long = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, n)
        }.timeInMillis

        return listOf(
            // ==============================================================
            // HARD TIER (10 entries · IDs 200..209)
            // ==============================================================
            ChallengeEntity(
                id = 200, title = "Dậy lúc 5 giờ sáng — 30 ngày",
                description = "Bạn không thể trở thành phiên bản tốt hơn nếu vẫn ngủ đến trưa. 30 ngày dậy lúc 5 giờ sáng sẽ định hình lại nhịp sinh học, cho bạn 2 giờ vàng mỗi ngày — trước cả khi thế giới thức dậy.",
                short_description = "5h sáng × 30 ngày liên tiếp",
                motivational_quote = "Người chiến thắng không thức dậy sớm — họ thức dậy sớm để chiến thắng.",
                completion_message = "Chuông báo thức không còn là kẻ thù. Bạn đã làm chủ buổi sáng.",
                category_id = 5, difficulty = "HARD",
                duration_days = 30, target_streak = 30, reward_coins = 600, reward_badge_id = 31,
                icon_emoji = "🌅", color_hex = "#F97316", is_featured = true,
                participant_count = 8400, sort_order = 200, created_at = now
            ),
            ChallengeEntity(
                id = 201, title = "10.000 bước mỗi ngày",
                description = "Một con số không cao siêu, nhưng sự thật là 90% mọi người không đạt được. 30 ngày đi đủ 10.000 bước biến cơ thể bạn thành cỗ máy đốt năng lượng và tinh thần thành thứ vũ khí.",
                short_description = "10.000 bước × 30 ngày",
                motivational_quote = "Bước chân hôm nay là bản thân ngày mai.",
                completion_message = "Bạn vừa đi qua hơn 200km bằng chính ý chí của mình.",
                category_id = 1, difficulty = "HARD",
                duration_days = 30, target_streak = 30, reward_coins = 550, reward_badge_id = 32,
                icon_emoji = "👟", color_hex = "#EF4444",
                participant_count = 12300, sort_order = 201, created_at = now
            ),
            ChallengeEntity(
                id = 202, title = "Deep Work 4 giờ mỗi ngày",
                description = "21 ngày tập trung sâu 4 giờ liên tục mỗi ngày — không Slack, không TikTok, không thông báo. Đây là kỷ luật phân biệt người làm việc và người tạo ra giá trị thực sự.",
                short_description = "4h deep work × 21 ngày",
                motivational_quote = "Sự tập trung là kỹ năng siêu việt của thế kỷ 21.",
                completion_message = "Bạn vừa giành lại 84 giờ chất lượng cao mà người khác sẽ không bao giờ có.",
                category_id = 4, difficulty = "HARD",
                duration_days = 21, target_streak = 21, reward_coins = 500, reward_badge_id = 33,
                icon_emoji = "🎯", color_hex = "#6366F1", is_featured = true,
                participant_count = 7800, sort_order = 202, created_at = now
            ),
            ChallengeEntity(
                id = 203, title = "21 ngày không đồ ăn vặt",
                description = "Đường, dầu mỡ, đồ chế biến sẵn — tất cả ra khỏi danh sách trong 21 ngày liên tiếp. Cơ thể bạn sẽ phản kháng trong tuần đầu, biết ơn bạn ở tuần thứ ba.",
                short_description = "Không junk food × 21 ngày",
                motivational_quote = "Mỗi lần từ chối đồ ăn vặt là một lần đầu tư cho phiên bản tốt hơn của bạn.",
                completion_message = "Bạn đã tái thiết khẩu vị và kỷ luật ăn uống.",
                category_id = 2, difficulty = "HARD",
                duration_days = 21, target_streak = 21, reward_coins = 450, reward_badge_id = 34,
                icon_emoji = "🥬", color_hex = "#22C55E",
                participant_count = 6500, sort_order = 203, created_at = now
            ),
            ChallengeEntity(
                id = 204, title = "Tắm nước lạnh — 30 ngày",
                description = "30 ngày tắm nước lạnh khi vừa thức dậy. Không thoả hiệp, không bù vào buổi tối. Đây là khoảnh khắc bạn dạy não bộ rằng cảm giác khó chịu không phải là lý do để dừng lại.",
                short_description = "Cold shower × 30 ngày sáng",
                motivational_quote = "Ai làm chủ được 5 phút khó khăn nhất của ngày, sẽ làm chủ cả ngày.",
                completion_message = "Hệ thần kinh của bạn vừa lên một cấp độ hoàn toàn mới.",
                category_id = 5, difficulty = "HARD",
                duration_days = 30, target_streak = 30, reward_coins = 550, reward_badge_id = 34,
                icon_emoji = "🚿", color_hex = "#0EA5E9",
                participant_count = 4900, sort_order = 204, created_at = now,
                start_date = inDays(3)
            ),
            ChallengeEntity(
                id = 205, title = "Tập luyện mỗi ngày — 30 ngày",
                description = "30 ngày tập luyện liên tục, tối thiểu 30 phút mỗi ngày. Nghỉ một ngày là về số 0. Sự nhất quán này sẽ phân định bạn ra khỏi 95% còn lại.",
                short_description = "Workout × 30 ngày liên tục",
                motivational_quote = "Cơ thể là tài sản duy nhất bạn không thể mua mới.",
                completion_message = "Đây không còn là thử thách — đây đã là phong cách sống của bạn.",
                category_id = 1, difficulty = "HARD",
                duration_days = 30, target_streak = 30, reward_coins = 600, reward_badge_id = 32,
                icon_emoji = "💪", color_hex = "#DC2626",
                participant_count = 9100, sort_order = 205, created_at = now
            ),
            ChallengeEntity(
                id = 206, title = "Đọc sách 30 ngày liên tiếp",
                description = "30 ngày, mỗi ngày tối thiểu 30 phút đọc sách. Không TikTok, không YouTube giải trí — chỉ sách. Bạn sẽ kết thúc với 3-5 cuốn sách và một bộ não khác hẳn.",
                short_description = "30 phút đọc × 30 ngày",
                motivational_quote = "Mỗi cuốn sách là một cuộc trò chuyện với người thông minh nhất bạn chưa từng gặp.",
                completion_message = "Bạn đã đầu tư hơn 15 giờ vào trí tuệ của chính mình. Lãi kép sẽ kéo dài hàng năm.",
                category_id = 4, difficulty = "HARD",
                duration_days = 30, target_streak = 30, reward_coins = 500, reward_badge_id = 35,
                icon_emoji = "📖", color_hex = "#14B8A6",
                participant_count = 8200, sort_order = 206, created_at = now
            ),
            ChallengeEntity(
                id = 207, title = "Thiền sâu — chuỗi 30 ngày",
                description = "30 phút thiền sâu mỗi ngày trong 30 ngày liên tiếp. Không bỏ ngày nào. Đây không phải bài tập thư giãn — đây là rèn luyện tâm trí cường độ cao.",
                short_description = "30 phút thiền × 30 ngày",
                motivational_quote = "Tâm trí bình lặng là siêu năng lực thực sự.",
                completion_message = "Bạn đã giành lại quyền điều khiển tâm trí mình.",
                category_id = 3, difficulty = "HARD",
                duration_days = 30, target_streak = 30, reward_coins = 500, reward_badge_id = 35,
                icon_emoji = "🧘", color_hex = "#7C3AED",
                participant_count = 5600, sort_order = 207, created_at = now,
                start_date = inDays(7)
            ),
            ChallengeEntity(
                id = 208, title = "Code mỗi ngày — 30 ngày",
                description = "30 ngày liên tiếp viết code, tối thiểu 1 commit có ý nghĩa mỗi ngày. Không tính 'fix typo'. Đây là cách lập trình viên nghiêm túc xây dựng portfolio thực sự.",
                short_description = "Coding streak × 30 ngày",
                motivational_quote = "Code mỗi ngày, dù là một dòng. Đó là cách kỹ năng được tôi luyện.",
                completion_message = "30 commits có ý nghĩa — bạn vừa xây dựng một thói quen công nghệ thực thụ.",
                category_id = 4, difficulty = "HARD",
                duration_days = 30, target_streak = 30, reward_coins = 550, reward_badge_id = 33,
                icon_emoji = "💻", color_hex = "#6366F1",
                participant_count = 4300, sort_order = 208, created_at = now
            ),
            ChallengeEntity(
                id = 209, title = "Productivity Reset — 21 ngày",
                description = "21 ngày tổng hợp: dậy sớm, plan ngày, deep work, tập thể dục, ngủ trước 23h. Một cuộc reset toàn diện cho năng suất cá nhân của bạn.",
                short_description = "Reset 21 ngày toàn diện",
                motivational_quote = "Bạn không cần thêm thời gian — bạn cần thêm kỷ luật để tận dụng nó.",
                completion_message = "Bạn vừa cài đặt lại cách bạn vận hành mỗi ngày.",
                category_id = 5, difficulty = "HARD",
                duration_days = 21, target_streak = 21, reward_coins = 600, reward_badge_id = 31,
                icon_emoji = "🚀", color_hex = "#0F172A", is_featured = true,
                participant_count = 7100, sort_order = 209, created_at = now
            ),

            // ==============================================================
            // LEGENDARY TIER (10 entries · IDs 210..219)
            // ==============================================================
            ChallengeEntity(
                id = 210, title = "75 Hard — Phiên bản BetterMe",
                description = "75 ngày: 2 lần tập (1 ngoài trời), uống 4 lít nước, đọc sách phát triển bản thân 10 trang, ăn sạch tuyệt đối, không cồn, chụp ảnh tiến độ. Bỏ một ngày là về số 0.",
                short_description = "75 ngày — không thoả hiệp",
                motivational_quote = "Đây không phải thử thách thể chất. Đây là chiến tranh với phiên bản yếu đuối của bạn.",
                completion_message = "Bạn vừa hoàn thành điều mà 99% người khác chỉ dám nghĩ đến.",
                category_id = 5, difficulty = "LEGENDARY",
                duration_days = 75, target_streak = 75, reward_coins = 2500, reward_badge_id = 36,
                icon_emoji = "⚔️", color_hex = "#0F172A", is_featured = true,
                participant_count = 1800, sort_order = 210, created_at = now
            ),
            ChallengeEntity(
                id = 211, title = "Monk Mode — 30 ngày",
                description = "30 ngày sống như tu sĩ: dậy sớm, thiền, không mạng xã hội, không giải trí thừa, ăn đơn giản, ngủ sớm. Trả lại cho não bộ trạng thái bạn từng có trước thế giới phẳng.",
                short_description = "Monk lifestyle × 30 ngày",
                motivational_quote = "Khi bạn loại bỏ tiếng ồn, sự minh mẫn xuất hiện.",
                completion_message = "Bạn vừa tìm lại bản thân mà internet đã ăn cắp.",
                category_id = 3, difficulty = "LEGENDARY",
                duration_days = 30, target_streak = 30, reward_coins = 1500, reward_badge_id = 37,
                icon_emoji = "🧘‍♂️", color_hex = "#7C3AED",
                participant_count = 2400, sort_order = 211, created_at = now,
                start_date = inDays(14)
            ),
            ChallengeEntity(
                id = 212, title = "60 ngày không mạng xã hội",
                description = "60 ngày liên tiếp xoá hoặc khoá Facebook, Instagram, TikTok, X. Trả lại bộ não 4-6 giờ mỗi ngày. Đây là cách bạn lấy lại cuộc sống của mình.",
                short_description = "Detox MXH × 60 ngày",
                motivational_quote = "Mỗi giờ trên mạng xã hội là một giờ ai đó đang xây tương lai của họ.",
                completion_message = "Bạn vừa giành lại 250+ giờ. Hãy dùng chúng để xây điều gì đó vĩ đại.",
                category_id = 5, difficulty = "LEGENDARY",
                duration_days = 60, target_streak = 60, reward_coins = 2000, reward_badge_id = 38,
                icon_emoji = "🚫", color_hex = "#DC2626",
                participant_count = 3100, sort_order = 212, created_at = now
            ),
            ChallengeEntity(
                id = 213, title = "Lịch trình vận động viên ưu tú — 45 ngày",
                description = "45 ngày tập theo lịch trình của vận động viên chuyên nghiệp: 2 buổi tập/ngày, ăn theo macro, ngủ 8 tiếng, hồi phục có kỷ luật. Cơ thể của bạn sẽ thay đổi tận gốc.",
                short_description = "Elite athlete × 45 ngày",
                motivational_quote = "Bạn không vô tình trở thành phiên bản tốt nhất — bạn được rèn luyện thành.",
                completion_message = "Bạn vừa nâng cấp cơ thể mình lên cấp độ mà 1% top thế giới đang ở.",
                category_id = 1, difficulty = "LEGENDARY",
                duration_days = 45, target_streak = 45, reward_coins = 1800, reward_badge_id = 39,
                icon_emoji = "🏋️", color_hex = "#EF4444",
                participant_count = 1500, sort_order = 213, created_at = now
            ),
            ChallengeEntity(
                id = 214, title = "Hành trình lột xác 90 ngày",
                description = "90 ngày: thân thể, tâm trí, tài chính, mối quan hệ — refactor toàn diện. Mỗi tuần một mục tiêu nhỏ, mỗi tháng một bước nhảy lớn. Đây là cuộc tái sinh có chủ đích.",
                short_description = "Transformation × 90 ngày",
                motivational_quote = "Lột xác không phải sự kiện — đó là kết quả của 90 quyết định nhỏ mỗi ngày.",
                completion_message = "Người ngày hôm qua sẽ không nhận ra người bạn của hôm nay.",
                category_id = 5, difficulty = "LEGENDARY",
                duration_days = 90, target_streak = 90, reward_coins = 2800, reward_badge_id = 39,
                icon_emoji = "🦅", color_hex = "#F59E0B", is_featured = true,
                participant_count = 2700, sort_order = 214, created_at = now
            ),
            ChallengeEntity(
                id = 215, title = "100 ngày kỷ luật tuyệt đối",
                description = "100 ngày — không bỏ một ngày nào. Một thói quen cốt lõi do bạn chọn (đọc, viết, tập, thiền). Đây là phép thử cuối cùng để biết bạn có thể tin tưởng vào chính mình.",
                short_description = "100 ngày × 1 thói quen lõi",
                motivational_quote = "Tin tưởng vào bản thân không phải cảm giác — đó là biên lai từ những lời hứa bạn đã giữ.",
                completion_message = "Bạn vừa xây dựng tài sản quan trọng nhất: lòng tin với chính mình.",
                category_id = 5, difficulty = "LEGENDARY",
                duration_days = 100, target_streak = 100, reward_coins = 3000, reward_badge_id = 38,
                icon_emoji = "💎", color_hex = "#0EA5E9",
                participant_count = 2200, sort_order = 215, created_at = now
            ),
            ChallengeEntity(
                id = 216, title = "Tâm trí thép — 60 ngày",
                description = "60 ngày rèn luyện tâm trí: thiền 30 phút, viết nhật ký 15 phút, tập trung sâu 3 giờ, lạnh nóng tương phản. Đây là cách bạn xây bộ não bất khả xâm phạm.",
                short_description = "Iron mind × 60 ngày",
                motivational_quote = "Tâm trí mạnh mẽ không tự sinh ra — nó được rèn trong lửa của lựa chọn khó khăn.",
                completion_message = "Bạn vừa nâng cấp lên hệ thần kinh của một người chiến thắng.",
                category_id = 3, difficulty = "LEGENDARY",
                duration_days = 60, target_streak = 60, reward_coins = 2200, reward_badge_id = 37,
                icon_emoji = "🧠", color_hex = "#0F172A",
                participant_count = 1900, sort_order = 216, created_at = now,
                start_date = inDays(21)
            ),
            ChallengeEntity(
                id = 217, title = "Tập trung cực hạn — 30 ngày",
                description = "30 ngày deep work 6 giờ/ngày, không gián đoạn. Đây là chế độ vận hành của những người tạo ra điều phi thường.",
                short_description = "Extreme focus × 30 ngày",
                motivational_quote = "Khi đám đông bị phân tán, bạn tập trung. Đó là lợi thế cạnh tranh thực sự.",
                completion_message = "Bạn vừa tích luỹ 180 giờ tập trung sâu — nhiều hơn nhiều người làm cả năm.",
                category_id = 4, difficulty = "LEGENDARY",
                duration_days = 30, target_streak = 30, reward_coins = 1800, reward_badge_id = 40,
                icon_emoji = "🎯", color_hex = "#6366F1",
                participant_count = 2000, sort_order = 217, created_at = now
            ),
            ChallengeEntity(
                id = 218, title = "Thói quen của triệu phú — 60 ngày",
                description = "60 ngày: dậy sớm, đọc 1 cuốn sách kinh doanh/tháng, tập thể dục, ghi journal về tài chính, học một kỹ năng tạo thu nhập. Đây không phải về tiền — đây là về cách suy nghĩ.",
                short_description = "Millionaire routine × 60 ngày",
                motivational_quote = "Triệu phú không là gì khác ngoài 1.000.000 quyết định đúng được lặp lại.",
                completion_message = "Bạn đang nghĩ và sống như tầng lớp 1%.",
                category_id = 4, difficulty = "LEGENDARY",
                duration_days = 60, target_streak = 60, reward_coins = 2400, reward_badge_id = 39,
                icon_emoji = "💰", color_hex = "#EAB308",
                participant_count = 2600, sort_order = 218, created_at = now
            ),
            ChallengeEntity(
                id = 219, title = "Self-Mastery huyền thoại — 90 ngày",
                description = "90 ngày của tự chủ tuyệt đối: cảm xúc, thời gian, năng lượng, thói quen, ngôn từ. Bạn không còn bị thế giới bên ngoài điều khiển — bạn điều khiển nó.",
                short_description = "Self-Mastery × 90 ngày",
                motivational_quote = "Người chinh phục được bản thân chính là người mạnh nhất.",
                completion_message = "Bạn vừa đạt cảnh giới mà phần lớn nhân loại sẽ không bao giờ chạm tới.",
                category_id = 5, difficulty = "LEGENDARY",
                duration_days = 90, target_streak = 90, reward_coins = 3500, reward_badge_id = 40,
                icon_emoji = "👑", color_hex = "#F59E0B", is_featured = true,
                participant_count = 1200, sort_order = 219, created_at = now,
                start_date = inDays(30)
            )
        )
    }
}

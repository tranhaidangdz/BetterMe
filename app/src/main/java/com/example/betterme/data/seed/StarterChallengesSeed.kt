package com.example.betterme.data.seed

import com.example.betterme.data.local.room.entities.ChallengeEntity
import java.util.Calendar

/**
 * EASY + MEDIUM tier expansion.
 *
 * 20 entries — 10 EASY (IDs 300..309) and 10 MEDIUM (IDs 310..319) — sharing the same
 * [ChallengeEntity] schema as [ChallengesSeed], [UpcomingChallengesSeed] and
 * [EliteChallengesSeed]. There is no separate "starter" runtime model: these flow
 * through the same Discover / Overview / Detail screens, get joined the same way,
 * persist progress in the same tables.
 *
 * Distribution across the existing 6 categories
 *   1 = Vận động & thể chất (Fitness)
 *   2 = Dinh dưỡng (Health / Nutrition)
 *   3 = Tinh thần (Mindfulness)
 *   4 = Học tập (Learning / Productivity)
 *   5 = Kỷ luật (Discipline)
 *   6 = Mối quan hệ (Relationships)
 *
 * EASY entries focus on beginner habits, low-friction wins, motivation building.
 * MEDIUM entries step up to stronger discipline, productivity, focus and lifestyle
 * improvements. Reward coins scale modestly (60-180 EASY, 200-400 MEDIUM) so this tier
 * stays distinct from the elite-tier rewards (450+).
 *
 * Reward badges reuse the existing pool (BadgesSeed IDs 1..30) — no new badges added.
 */
object StarterChallengesSeed {

    fun starter(now: Long = System.currentTimeMillis()): List<ChallengeEntity> {
        fun inDays(n: Int): Long = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, n)
        }.timeInMillis

        return listOf(
            // ==============================================================
            // EASY TIER (10 entries · IDs 300..309)
            // ==============================================================
            ChallengeEntity(
                id = 300, title = "Một ly nước ngay khi thức dậy",
                description = "Cơ thể bạn vừa nhịn nước 7-8 tiếng khi ngủ. Một ly nước ấm trong vòng 5 phút đầu tiên sau khi thức dậy là cách đơn giản nhất để khởi động lại toàn bộ hệ trao đổi chất.",
                short_description = "1 ly nước sau khi thức dậy × 7 ngày",
                motivational_quote = "Thói quen nhỏ, hiệu ứng lớn — bắt đầu từ ly nước đầu tiên.",
                completion_message = "Bạn vừa xây xong một thói quen sức khoẻ mà 80% người đời quên mất.",
                category_id = 2, difficulty = "EASY",
                duration_days = 7, target_streak = 7, reward_coins = 60, reward_badge_id = 1,
                icon_emoji = "💧", color_hex = "#3B82F6",
                participant_count = 14200, sort_order = 300, created_at = now
            ),
            ChallengeEntity(
                id = 301, title = "Đọc 5 trang sách mỗi ngày",
                description = "5 trang. Không nhiều, không ít. Đủ để duy trì thói quen mà không tạo áp lực. Trong 14 ngày, bạn đọc xong khoảng 70 trang — gần một phần ba cuốn sách trung bình.",
                short_description = "5 trang × 14 ngày",
                motivational_quote = "Mỗi trang sách là một viên gạch xây dựng tâm trí.",
                completion_message = "Bạn vừa biến đọc sách thành một phần của ngày — phần khó nhất là bắt đầu.",
                category_id = 4, difficulty = "EASY",
                duration_days = 14, target_streak = 14, reward_coins = 100, reward_badge_id = 11,
                icon_emoji = "📖", color_hex = "#14B8A6",
                participant_count = 11800, sort_order = 301, created_at = now
            ),
            ChallengeEntity(
                id = 302, title = "Dậy đúng giờ 7 ngày liên tiếp",
                description = "Đặt một giờ thức dậy cố định trong 7 ngày — và giữ vững. Không snooze, không thoả hiệp. Đây là viên gạch đầu tiên của kỷ luật.",
                short_description = "Dậy đúng giờ × 7 ngày",
                motivational_quote = "Người làm chủ buổi sáng làm chủ cả ngày.",
                completion_message = "Báo thức không còn là kẻ thù — bạn đã thắng nó 7 lần liên tiếp.",
                category_id = 5, difficulty = "EASY",
                duration_days = 7, target_streak = 7, reward_coins = 80, reward_badge_id = 21,
                icon_emoji = "⏰", color_hex = "#F97316", is_featured = true,
                participant_count = 13500, sort_order = 302, created_at = now
            ),
            ChallengeEntity(
                id = 303, title = "Đi bộ 15 phút mỗi ngày",
                description = "15 phút — bằng một tập podcast ngắn. Không cần phòng gym, không cần thiết bị. Chỉ cần ra khỏi ghế và bước đi. Cơ thể và đầu óc bạn sẽ cảm ơn bạn.",
                short_description = "Đi bộ 15 phút × 14 ngày",
                motivational_quote = "Sức khoẻ không bắt đầu từ phòng gym — bắt đầu từ bước chân đầu tiên.",
                completion_message = "Bạn đã chứng minh: vận động không cần hoành tráng, chỉ cần đều đặn.",
                category_id = 1, difficulty = "EASY",
                duration_days = 14, target_streak = 14, reward_coins = 100, reward_badge_id = 6,
                icon_emoji = "🚶", color_hex = "#10B981",
                participant_count = 16200, sort_order = 303, created_at = now
            ),
            ChallengeEntity(
                id = 304, title = "3 điều biết ơn mỗi ngày",
                description = "Mỗi tối, viết ra 3 điều bạn biết ơn trong ngày. Không cần lớn lao — một tách cà phê ngon, một cuộc trò chuyện vui, một khoảnh khắc nắng đẹp đều đáng được ghi nhận.",
                short_description = "3 lời biết ơn × 7 ngày",
                motivational_quote = "Lòng biết ơn là cánh cửa đầu tiên dẫn đến hạnh phúc.",
                completion_message = "Bạn vừa rèn cho bộ não thói quen tìm điều tốt — và sẽ tìm thấy nhiều hơn.",
                category_id = 3, difficulty = "EASY",
                duration_days = 7, target_streak = 7, reward_coins = 70, reward_badge_id = 27,
                icon_emoji = "🙏", color_hex = "#A78BFA",
                participant_count = 9800, sort_order = 304, created_at = now
            ),
            ChallengeEntity(
                id = 305, title = "Stretching 5 phút buổi sáng",
                description = "5 phút giãn cơ ngay khi vừa thức dậy. Cơ thể vừa qua 7 tiếng bất động — một vài động tác đơn giản đánh thức cơ và khớp, cho bạn năng lượng cả ngày.",
                short_description = "Stretching 5 phút × 14 ngày",
                motivational_quote = "Cơ thể linh hoạt là nền tảng của một ngày năng động.",
                completion_message = "Bạn đã cài đặt một ritual buổi sáng đơn giản nhưng quyền năng.",
                category_id = 1, difficulty = "EASY",
                duration_days = 14, target_streak = 14, reward_coins = 100, reward_badge_id = 7,
                icon_emoji = "🤸", color_hex = "#22C55E",
                participant_count = 8400, sort_order = 305, created_at = now
            ),
            ChallengeEntity(
                id = 306, title = "Tắt điện thoại 30 phút trước khi ngủ",
                description = "Ánh sáng xanh từ màn hình ức chế melatonin — hormone giúp bạn ngủ ngon. 30 phút không màn hình trước khi ngủ là cách đơn giản nhất để cải thiện chất lượng giấc ngủ.",
                short_description = "Không màn hình 30' trước ngủ × 14 ngày",
                motivational_quote = "Một giấc ngủ tốt bắt đầu từ 30 phút trước khi nhắm mắt.",
                completion_message = "Bạn vừa giành lại điều quý nhất — một giấc ngủ thật sự sâu.",
                category_id = 5, difficulty = "EASY",
                duration_days = 14, target_streak = 14, reward_coins = 120, reward_badge_id = 22,
                icon_emoji = "📵", color_hex = "#0EA5E9",
                participant_count = 10500, sort_order = 306, created_at = now
            ),
            ChallengeEntity(
                id = 307, title = "Một bữa rau xanh mỗi ngày",
                description = "Mỗi ngày một bữa có rau xanh tươi. Không cần ăn chay, không cần chế độ phức tạp — chỉ cần đảm bảo dĩa rau có mặt trong ít nhất một bữa.",
                short_description = "1 bữa rau × 7 ngày",
                motivational_quote = "Cơ thể bạn được tạo từ những gì bạn ăn — hôm nay và 30 ngày tới.",
                completion_message = "Bạn đã đặt nền móng cho một thói quen ăn uống lành mạnh.",
                category_id = 2, difficulty = "EASY",
                duration_days = 7, target_streak = 7, reward_coins = 70, reward_badge_id = 8,
                icon_emoji = "🥗", color_hex = "#16A34A",
                participant_count = 7600, sort_order = 307, created_at = now
            ),
            ChallengeEntity(
                id = 308, title = "Hít thở sâu 5 phút mỗi ngày",
                description = "5 phút hít thở sâu — tim đập chậm lại, vai thả lỏng, đầu óc trong trẻo hơn. Không cần thiền cao siêu, chỉ cần 5 phút tập trung vào hơi thở.",
                short_description = "Thở sâu 5' × 7 ngày",
                motivational_quote = "Hơi thở là chiếc neo đưa bạn về hiện tại.",
                completion_message = "Bạn vừa khám phá công cụ thư giãn miễn phí và mạnh nhất bạn có.",
                category_id = 3, difficulty = "EASY",
                duration_days = 7, target_streak = 7, reward_coins = 60, reward_badge_id = 28,
                icon_emoji = "🌬️", color_hex = "#7C3AED",
                participant_count = 6900, sort_order = 308, created_at = now,
                start_date = inDays(2)
            ),
            ChallengeEntity(
                id = 309, title = "Nhật ký 3 dòng cuối ngày",
                description = "Mỗi tối, viết 3 dòng: hôm nay đã làm được điều gì, hôm nay học được gì, ngày mai muốn làm gì. Đơn giản, không tốn thời gian, nhưng tạo thói quen tự phản tỉnh.",
                short_description = "Nhật ký 3 dòng × 14 ngày",
                motivational_quote = "Cuộc sống không nhìn lại là cuộc sống không học hỏi.",
                completion_message = "14 ngày tự phản tỉnh — bạn vừa biết bản thân hơn so với cả năm trước.",
                category_id = 3, difficulty = "EASY",
                duration_days = 14, target_streak = 14, reward_coins = 120, reward_badge_id = 29,
                icon_emoji = "📝", color_hex = "#FBBF24",
                participant_count = 8100, sort_order = 309, created_at = now
            ),

            // ==============================================================
            // MEDIUM TIER (10 entries · IDs 310..319)
            // ==============================================================
            ChallengeEntity(
                id = 310, title = "Tập luyện 30 phút × 4 lần/tuần",
                description = "21 ngày, mỗi tuần 4 buổi tập tối thiểu 30 phút. Yoga, gym, chạy bộ, calisthenics — bạn chọn. Đây là thử thách đầu tiên thực sự yêu cầu sự cam kết với cơ thể của bạn.",
                short_description = "4 buổi tập/tuần × 21 ngày",
                motivational_quote = "Sự nhất quán biến đổi cơ thể nhiều hơn cường độ.",
                completion_message = "12 buổi tập đã được tích luỹ — bạn đã trở thành người vận động đều đặn.",
                category_id = 1, difficulty = "MEDIUM",
                duration_days = 21, target_streak = 12, reward_coins = 250, reward_badge_id = 9,
                icon_emoji = "🏋️", color_hex = "#EF4444", is_featured = true,
                participant_count = 11400, sort_order = 310, created_at = now
            ),
            ChallengeEntity(
                id = 311, title = "Không cà phê sau 14 giờ",
                description = "14 ngày không uống cà phê (hay trà đen, trà xanh) sau 14 giờ chiều. Cải thiện chất lượng giấc ngủ ngay từ tuần đầu — bạn sẽ ngạc nhiên mình ngủ sâu thế nào.",
                short_description = "Không cà phê sau 14h × 14 ngày",
                motivational_quote = "Giấc ngủ chất lượng bắt đầu từ buổi chiều.",
                completion_message = "Bạn vừa hack giấc ngủ một cách đơn giản nhưng cực kỳ hiệu quả.",
                category_id = 2, difficulty = "MEDIUM",
                duration_days = 14, target_streak = 14, reward_coins = 220, reward_badge_id = 23,
                icon_emoji = "☕", color_hex = "#92400E",
                participant_count = 6800, sort_order = 311, created_at = now
            ),
            ChallengeEntity(
                id = 312, title = "Đọc sách 30 phút mỗi ngày",
                description = "21 ngày đọc 30 phút mỗi ngày — không TikTok, không YouTube giải trí thay thế. Bạn sẽ kết thúc với 1-2 cuốn sách hoàn chỉnh và thói quen đọc đã được lập trình.",
                short_description = "Đọc sách 30' × 21 ngày",
                motivational_quote = "Người đọc nhiều luôn nhìn thấy nhiều hơn.",
                completion_message = "Bạn vừa đầu tư hơn 10 giờ vào trí tuệ của chính mình.",
                category_id = 4, difficulty = "MEDIUM",
                duration_days = 21, target_streak = 21, reward_coins = 280, reward_badge_id = 12,
                icon_emoji = "📚", color_hex = "#0F766E",
                participant_count = 9600, sort_order = 312, created_at = now
            ),
            ChallengeEntity(
                id = 313, title = "Lên kế hoạch ngày tối hôm trước",
                description = "Mỗi tối, dành 5-10 phút viết ra 3 nhiệm vụ quan trọng nhất cho ngày mai. Buổi sáng bạn không phải nghĩ ngợi — chỉ cần thực thi.",
                short_description = "Plan trước 1 ngày × 14 ngày",
                motivational_quote = "Một kế hoạch nghèo còn hơn không có kế hoạch.",
                completion_message = "Bạn vừa nâng cấp năng suất bằng cách lên lịch cho thành công.",
                category_id = 5, difficulty = "MEDIUM",
                duration_days = 14, target_streak = 14, reward_coins = 200, reward_badge_id = 24,
                icon_emoji = "📋", color_hex = "#6366F1",
                participant_count = 7900, sort_order = 313, created_at = now
            ),
            ChallengeEntity(
                id = 314, title = "Học tiếng Anh 20 phút/ngày",
                description = "21 ngày, mỗi ngày 20 phút học tiếng Anh — Duolingo, podcast, đọc bài viết. Đủ ngắn để không nản, đủ đều để não bắt đầu nhớ thật.",
                short_description = "Học tiếng Anh 20' × 21 ngày",
                motivational_quote = "Một ngôn ngữ mới là một thế giới mới.",
                completion_message = "Bạn đã tích luỹ 7 giờ học — đủ để cảm nhận sự tiến bộ rõ rệt.",
                category_id = 4, difficulty = "MEDIUM",
                duration_days = 21, target_streak = 21, reward_coins = 280, reward_badge_id = 13,
                icon_emoji = "🌍", color_hex = "#8B5CF6",
                participant_count = 10200, sort_order = 314, created_at = now,
                start_date = inDays(5)
            ),
            ChallengeEntity(
                id = 315, title = "Không đường thêm trong 14 ngày",
                description = "14 ngày không bánh ngọt, không nước ngọt, không thêm đường vào cà phê. Cảm giác thèm sẽ giảm đáng kể vào tuần thứ hai. Khẩu vị của bạn được reset.",
                short_description = "Không đường thêm × 14 ngày",
                motivational_quote = "Đường ngọt nhưng cuộc sống không cần đường để ngọt.",
                completion_message = "Bạn vừa lấy lại quyền điều khiển khẩu vị.",
                category_id = 2, difficulty = "MEDIUM",
                duration_days = 14, target_streak = 14, reward_coins = 240, reward_badge_id = 10,
                icon_emoji = "🚫🍰", color_hex = "#EC4899",
                participant_count = 5400, sort_order = 315, created_at = now
            ),
            ChallengeEntity(
                id = 316, title = "Tập trung 90 phút không phân tâm",
                description = "14 ngày, mỗi ngày 1 khối tập trung sâu 90 phút — không thông báo, không tab khác, không điện thoại. Đây là cường độ tập trung mà 95% người làm việc văn phòng chưa bao giờ chạm đến.",
                short_description = "Deep focus 90' × 14 ngày",
                motivational_quote = "Tập trung là siêu năng lực hiếm nhất của thời đại.",
                completion_message = "Bạn vừa hoàn thành 21 giờ tập trung sâu — nhiều hơn người khác làm trong cả tháng.",
                category_id = 4, difficulty = "MEDIUM",
                duration_days = 14, target_streak = 14, reward_coins = 300, reward_badge_id = 25,
                icon_emoji = "🎯", color_hex = "#0F172A", is_featured = true,
                participant_count = 6300, sort_order = 316, created_at = now
            ),
            ChallengeEntity(
                id = 317, title = "Ngủ trước 23h liên tiếp 21 ngày",
                description = "21 ngày liên tiếp đi ngủ trước 23h. Đây là khoản đầu tư quan trọng nhất bạn có thể làm cho sức khoẻ tinh thần và năng suất ban ngày.",
                short_description = "Ngủ trước 23h × 21 ngày",
                motivational_quote = "Ngủ sớm không phải lười — đó là kỷ luật cao nhất.",
                completion_message = "Bạn đã reset đồng hồ sinh học và tìm lại ban ngày của chính mình.",
                category_id = 5, difficulty = "MEDIUM",
                duration_days = 21, target_streak = 21, reward_coins = 280, reward_badge_id = 14,
                icon_emoji = "🌙", color_hex = "#1E40AF",
                participant_count = 8700, sort_order = 317, created_at = now
            ),
            ChallengeEntity(
                id = 318, title = "Không mạng xã hội trước trưa",
                description = "14 ngày không mở Facebook, Instagram, TikTok trước 12 giờ trưa. Buổi sáng yên tĩnh — bạn sẽ ngạc nhiên mình tập trung bao lâu sau khi loại bỏ tiếng ồn này.",
                short_description = "Không MXH trước trưa × 14 ngày",
                motivational_quote = "Buổi sáng yên tĩnh là bí mật của những người làm được nhiều việc.",
                completion_message = "Bạn vừa giành lại 4-5 giờ chất lượng cao mỗi ngày.",
                category_id = 5, difficulty = "MEDIUM",
                duration_days = 14, target_streak = 14, reward_coins = 250, reward_badge_id = 30,
                icon_emoji = "🌅", color_hex = "#FB923C",
                participant_count = 7400, sort_order = 318, created_at = now
            ),
            ChallengeEntity(
                id = 319, title = "7000 bước mỗi ngày",
                description = "21 ngày đi bộ tối thiểu 7000 bước/ngày. Vừa đủ để cảm nhận tác động đến sức khoẻ tim mạch và tâm trạng, không quá khó để duy trì cùng công việc.",
                short_description = "7000 bước × 21 ngày",
                motivational_quote = "Cơ thể được thiết kế để di chuyển — hãy tôn trọng nó.",
                completion_message = "Bạn vừa đi qua 100km bằng chính ý chí của mình.",
                category_id = 1, difficulty = "MEDIUM",
                duration_days = 21, target_streak = 21, reward_coins = 280, reward_badge_id = 6,
                icon_emoji = "👟", color_hex = "#F59E0B",
                participant_count = 12600, sort_order = 319, created_at = now
            )
        )
    }
}

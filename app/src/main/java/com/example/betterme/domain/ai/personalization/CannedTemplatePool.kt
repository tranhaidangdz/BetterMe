package com.example.betterme.domain.ai.personalization

import com.example.betterme.domain.ai.SuggestedHabit
import java.util.concurrent.ConcurrentHashMap

/**
 * Local template pool for canned (offline) AI content. Centralizes the
 * "what should we say when OpenRouter is unreachable" decision so:
 *
 *  - Two users in different groups never see the same canned review.
 *  - The same user revisiting the same group gets a *different* canned
 *    template each time (rotation by category short-circuit, not random
 *    — predictable for testing, varied enough for the user).
 *  - Suggestions are category-aware AND signal-aware: a Fitness user
 *    flagged OVERLOADED gets recovery/stretching ideas, not more cardio.
 *
 * The pool itself is stateless data; the rotation index is held in a
 * process-scoped ConcurrentHashMap (`reviewIndex` / `suggestionIndex`)
 * keyed by `(category, surface)`. State is intentionally lost on
 * process death — a fresh start picking the first template again is
 * fine; canned-mode users only see this at all when their network is
 * down.
 */
object CannedTemplatePool {

    // ============================================================
    // REVIEW POOL (per-category × per-trend, with rotation)
    // ============================================================

    /**
     * Build a canned coaching paragraph for a [GroupInsightContext].
     * The result interpolates real habit titles + completion rates from
     * the context so the offline copy still references the user's actual
     * routine instead of reading like a horoscope.
     *
     * Picks one template from the pool indexed by [kind][CategoryKind] and
     * the user's [trendBucket]. Rotates by category to vary across visits.
     */
    fun pickReview(context: GroupInsightContext): String {
        val key = "${context.categoryKind.name}-${context.categoryName}"
        val pool = REVIEW_POOL[context.categoryKind] ?: REVIEW_POOL.getValue(CategoryKind.OTHER)
        val bucket = trendBucket(context)
        val variants = pool[bucket] ?: pool.getValue(TrendBucket.STABLE)
        val index = reviewIndex.compute(key) { _, prev -> ((prev ?: -1) + 1) % variants.size }!!
        val template = variants[index]
        return template.render(context)
    }

    // ============================================================
    // SUGGESTION POOL (per-category, signal-aware, dedup vs existing)
    // ============================================================

    /**
     * Build a canned 4-suggestion set for a [SuggestionContext].
     *
     *  1. Pulls the pool for [context.categoryKind] (falls back to OTHER
     *     when unknown).
     *  2. Filters out any suggestion whose title overlaps existing habits
     *     (case-insensitive substring match in either direction).
     *  3. When the user has [PersonalitySignal.OVERLOADED] or
     *     [PersonalitySignal.RECOVERY_NEEDING], swaps in recovery-oriented
     *     entries from the RECOVERY pool — adding more habits to a tired
     *     user is the opposite of what they need.
     *  4. Rotates by category so revisits show a different 4.
     */
    fun pickSuggestions(context: SuggestionContext): List<SuggestedHabit> {
        val basePool = SUGGESTION_POOL[context.categoryKind]
            ?: SUGGESTION_POOL.getValue(CategoryKind.OTHER)
        val recoveryPool = RECOVERY_OVERLAY
        val effective = if (
            PersonalitySignal.OVERLOADED in context.signals ||
            PersonalitySignal.RECOVERY_NEEDING in context.signals
        ) {
            // Replace half the pool with recovery picks so the user
            // doesn't get another cardio habit while overloaded.
            (recoveryPool.take(2) + basePool.drop(2)).distinctBy { it.title }
        } else basePool

        val filtered = effective.filterNot { candidate ->
            context.existingInCategory.any { existing ->
                titlesOverlap(existing, candidate.title)
            } || context.existingAcrossApp.any { existing ->
                titlesOverlap(existing, candidate.title)
            }
        }
        // Always return at least 3 — if filtering went too aggressive,
        // fall back to the bottom of the unfiltered pool so we never
        // serve an empty UI.
        val safe = if (filtered.size >= 3) filtered else (filtered + effective).distinctBy { it.title }

        val key = "${context.categoryKind.name}-${context.categoryName}"
        val window = ((suggestionIndex.compute(key) { _, prev -> ((prev ?: -1) + 1) }!!) % safe.size)
        // Rotate the start of the slice so visits show overlapping but
        // shifted picks — distinct enough to feel varied, similar enough
        // that the underlying intent stays coherent.
        return (safe.drop(window) + safe.take(window)).take(4)
    }

    /** Test/sign-out hook. */
    fun reset() {
        reviewIndex.clear()
        suggestionIndex.clear()
    }

    // ============================================================
    // Internals
    // ============================================================

    private val reviewIndex = ConcurrentHashMap<String, Int>()
    private val suggestionIndex = ConcurrentHashMap<String, Int>()

    private enum class TrendBucket { HIGH, STABLE, LOW, IMPROVING }

    private fun trendBucket(c: GroupInsightContext): TrendBucket = when {
        c.trendLabel == "improving" -> TrendBucket.IMPROVING
        c.overallRate >= 75 -> TrendBucket.HIGH
        c.overallRate < 40 -> TrendBucket.LOW
        else -> TrendBucket.STABLE
    }

    /**
     * Lightweight title-overlap check used for de-duping suggestions
     * against existing habits. Case-insensitive substring match in either
     * direction — "đi bộ" matches "Đi bộ 10.000 bước" and vice versa.
     */
    private fun titlesOverlap(a: String, b: String): Boolean {
        val la = a.trim().lowercase()
        val lb = b.trim().lowercase()
        if (la.isEmpty() || lb.isEmpty()) return false
        return la.contains(lb) || lb.contains(la)
    }

    /**
     * One review template — a short coaching paragraph with placeholders
     * the [render] step replaces from the live context. Keeping the
     * placeholders explicit (rather than free-form Kotlin string templates
     * at construction time) makes it trivial to add new variants without
     * touching the pool's structure.
     *
     * Placeholders:
     *  - `{topHabit}`   → first habit title from context
     *  - `{rate}`       → overall completion rate
     *  - `{streak}`     → best-streak figure
     *  - `{missed}`     → missed-days figure
     *  - `{habitCount}` → number of habits in the group
     */
    private data class ReviewTemplate(val body: String) {
        fun render(c: GroupInsightContext): String {
            val topHabit = c.habitTitles.firstOrNull() ?: c.categoryName
            return body
                .replace("{topHabit}", "\"$topHabit\"")
                .replace("{rate}", "${c.overallRate}%")
                .replace("{streak}", "${c.bestStreak}")
                .replace("{missed}", "${c.missedDays}")
                .replace("{habitCount}", "${c.habitCount}")
        }
    }

    // ── Review pool ─────────────────────────────────────────────
    // Three buckets × 2-3 templates each = 6-9 variants per category.
    // Each variant must reference at least one specific habit name via
    // `{topHabit}` so canned content lives up to the system prompt's
    // "mention real habits" rule.

    private val REVIEW_POOL: Map<CategoryKind, Map<TrendBucket, List<ReviewTemplate>>> = mapOf(
        CategoryKind.FITNESS to mapOf(
            TrendBucket.HIGH to listOf(
                ReviewTemplate("Nhịp tập luyện đang rất ổn — {topHabit} giữ vững ở {rate}% trong kỳ này. Hãy ưu tiên phục hồi nhẹ, đừng vội nâng cường độ quá nhanh."),
                ReviewTemplate("{topHabit} là điểm sáng của nhóm vận động ({rate}% hoàn thành). Một ngày nghỉ chủ động mỗi tuần sẽ giúp cơ thể bền hơn."),
                ReviewTemplate("Bạn đang giữ rất tốt nhóm vận động với chuỗi {streak} ngày — đừng quên giãn cơ và ngủ đủ để nhịp này kéo dài.")
            ),
            TrendBucket.STABLE to listOf(
                ReviewTemplate("Nhóm vận động đang ở mức {rate}% — không tệ, nhưng {topHabit} có vẻ chững lại. Thử rút ngắn thời lượng để dễ duy trì hơn."),
                ReviewTemplate("Tập luyện đang đều nhưng chưa đột phá. {topHabit} có thể là điểm bắt đầu để tăng độ ổn định, không phải độ khó."),
                ReviewTemplate("Vận động đang ổn định ({rate}%). Một nhịp nhỏ hơn, đều hơn quanh {topHabit} sẽ giúp bạn tiến xa hơn là cố gắng tăng cường.")
            ),
            TrendBucket.LOW to listOf(
                ReviewTemplate("Nhóm vận động đang khá đuối — bỏ lỡ {missed} ngày. {topHabit} có thể đang quá nặng so với nhịp hiện tại; thử phiên bản 10 phút."),
                ReviewTemplate("Có dấu hiệu quá tải trong nhóm vận động. Tạm dừng {topHabit} vài ngày và thay bằng đi bộ nhẹ sẽ giúp cơ thể hồi phục."),
                ReviewTemplate("Tỉ lệ {rate}% cho thấy lịch tập đang khó duy trì. Bắt đầu lại với một thói quen nhẹ hơn {topHabit} sẽ bền vững hơn.")
            ),
            TrendBucket.IMPROVING to listOf(
                ReviewTemplate("Tuần này bạn vận động đều hơn tuần trước — {topHabit} đang trên đà tốt. Giữ cường độ hiện tại trước khi tăng thêm."),
                ReviewTemplate("Có cải thiện rõ trong nhóm vận động: {topHabit} đang ổn định trở lại. Một bước nhỏ tiếp theo sẽ dễ vào nhịp.")
            )
        ),
        CategoryKind.STUDY to mapOf(
            TrendBucket.HIGH to listOf(
                ReviewTemplate("Học tập đang rất tập trung — {topHabit} duy trì {rate}%. Nhớ ngủ đủ; trí nhớ và sự tập trung phụ thuộc rất nhiều vào giấc ngủ."),
                ReviewTemplate("Bạn đang giữ nhịp học rất tốt với chuỗi {streak} ngày quanh {topHabit}. Một buổi tổng kết ngắn cuối tuần sẽ nhân đôi hiệu quả.")
            ),
            TrendBucket.STABLE to listOf(
                ReviewTemplate("Học tập đều đặn ở {rate}% — có thể {topHabit} chưa thật sự thử thách bạn. Thử thay đổi nội dung mỗi tuần để giữ sự hứng thú."),
                ReviewTemplate("Nhóm học đang ổn, nhưng cần đa dạng hơn quanh {topHabit}. Một chủ đề mới mỗi tuần sẽ kích thích trí não tốt hơn nhịp đều đều.")
            ),
            TrendBucket.LOW to listOf(
                ReviewTemplate("{topHabit} bỏ lỡ {missed} ngày — học muộn rất khó duy trì. Thử dời sớm hơn 1-2 tiếng và rút ngắn thời lượng."),
                ReviewTemplate("Nhóm học đang khó vào nhịp ({rate}%). Bắt đầu với 10 phút {topHabit} sẽ dễ thắng hơn là 60 phút.")
            ),
            TrendBucket.IMPROVING to listOf(
                ReviewTemplate("Tuần này bạn tập trung hơn tuần trước — {topHabit} đang vào guồng. Giữ thời lượng hiện tại thêm 1-2 tuần trước khi thử nhiều hơn.")
            )
        ),
        CategoryKind.SLEEP to mapOf(
            TrendBucket.HIGH to listOf(
                ReviewTemplate("Nhịp nghỉ ngơi đang rất ổn — {topHabit} giữ {rate}%. Đây là nền tảng cho mọi nhóm khác; đừng để công việc lấn vào giờ này."),
                ReviewTemplate("Bạn đang ngủ đúng nhịp với {topHabit}. Cố gắng giữ giờ đi ngủ cố định kể cả cuối tuần sẽ giúp đồng hồ sinh học ổn định.")
            ),
            TrendBucket.STABLE to listOf(
                ReviewTemplate("Giấc ngủ đang ổn ở mức {rate}% — có thể giờ {topHabit} chưa thực sự cố định. Một khung giờ nhất quán sẽ giúp ngủ sâu hơn."),
                ReviewTemplate("Nhóm nghỉ ngơi đều nhưng chưa thật sự đủ. Thử dời {topHabit} sớm hơn 15 phút mỗi tuần để cơ thể quen dần.")
            ),
            TrendBucket.LOW to listOf(
                ReviewTemplate("Giờ ngủ đang trôi muộn — {topHabit} bỏ lỡ {missed} ngày. Bắt đầu bằng việc tắt thiết bị sớm hơn 30 phút sẽ tạo hiệu ứng dây chuyền."),
                ReviewTemplate("Nhịp ngủ chưa ổn ({rate}%). {topHabit} có thể đang quá lý tưởng — thử đặt mục tiêu nhỏ hơn và giữ đều mỗi đêm.")
            ),
            TrendBucket.IMPROVING to listOf(
                ReviewTemplate("Giấc ngủ đang khá hơn tuần trước — {topHabit} bắt đầu vào nếp. Giữ giờ cố định để não bộ quen.")
            )
        ),
        CategoryKind.MINDFULNESS to mapOf(
            TrendBucket.HIGH to listOf(
                ReviewTemplate("Nhóm tinh thần đang rất vững — {topHabit} ở {rate}%. Đây là kỹ năng phục hồi quan trọng; giữ đều mỗi ngày dù chỉ 5 phút."),
                ReviewTemplate("Bạn đang chăm sóc tinh thần rất tốt với chuỗi {streak} ngày quanh {topHabit}. Thêm 1 phút thở sâu trước khi ngủ sẽ tăng hiệu ứng.")
            ),
            TrendBucket.STABLE to listOf(
                ReviewTemplate("Tinh thần đang ổn ở {rate}%. {topHabit} là chỗ neo đáng tin — thử thực hành cùng giờ mỗi ngày để vào trạng thái nhanh hơn."),
                ReviewTemplate("Nhóm tinh thần đang đều. Một biến thể ngắn của {topHabit} vào buổi sáng sẽ cân bằng nhịp căng thẳng buổi chiều.")
            ),
            TrendBucket.LOW to listOf(
                ReviewTemplate("{topHabit} bỏ lỡ {missed} ngày — căng thẳng dễ tăng khi nhóm tinh thần bị bỏ qua. Một phiên 3 phút mỗi sáng đã giúp ích."),
                ReviewTemplate("Nhóm tinh thần đang chững ({rate}%). Bắt đầu lại bằng 3 nhịp thở sâu trước {topHabit} sẽ thấy khác biệt nhanh.")
            ),
            TrendBucket.IMPROVING to listOf(
                ReviewTemplate("Tuần này tâm trí có vẻ tĩnh hơn — {topHabit} đang vào nhịp. Một câu cảm ơn ngắn trước khi ngủ sẽ neo cảm xúc tích cực.")
            )
        ),
        CategoryKind.NUTRITION to mapOf(
            TrendBucket.HIGH to listOf(
                ReviewTemplate("Nhóm dinh dưỡng đang rất chỉn chu — {topHabit} ở {rate}%. Đa dạng nguồn thực phẩm sẽ là bước tiếp theo tự nhiên."),
                ReviewTemplate("Bạn ăn uống đều và chủ động với chuỗi {streak} ngày quanh {topHabit}. Uống đủ nước giữa các bữa sẽ tăng hiệu quả.")
            ),
            TrendBucket.STABLE to listOf(
                ReviewTemplate("Dinh dưỡng đang ổn ({rate}%). {topHabit} là nền tảng — thử thêm một loại rau / trái cây mới mỗi tuần để tránh nhàm chán."),
                ReviewTemplate("Nhóm ăn uống đều nhưng chưa thật sự cân bằng. Thay đổi nhỏ ở {topHabit} (giảm đường / tăng đạm) sẽ tạo hiệu ứng rõ.")
            ),
            TrendBucket.LOW to listOf(
                ReviewTemplate("Bữa ăn bị bỏ lỡ {missed} ngày — cơ thể khó duy trì năng lượng khi {topHabit} không đều. Thử chuẩn bị sẵn từ tối hôm trước."),
                ReviewTemplate("Nhóm dinh dưỡng chưa vào nhịp ({rate}%). Một thay đổi nhỏ ở {topHabit} (ăn cùng giờ, cùng chỗ) thường hiệu quả hơn ép thực đơn.")
            ),
            TrendBucket.IMPROVING to listOf(
                ReviewTemplate("Tuần này bạn ăn uống đều hơn tuần trước — {topHabit} đang đi đúng hướng. Giữ giờ ăn cố định để dạ dày dễ chịu hơn.")
            )
        ),
        CategoryKind.FINANCE to mapOf(
            TrendBucket.STABLE to listOf(
                ReviewTemplate("Nhóm tài chính đang đều ({rate}%). {topHabit} là một thói quen rất đáng giữ — kết quả sẽ rõ sau vài tháng tích lũy."),
                ReviewTemplate("Quản lý chi tiêu đang ổn quanh {topHabit}. Một lần tổng kết cuối tuần ngắn sẽ giúp bạn thấy bức tranh tổng thể.")
            ),
            TrendBucket.HIGH to listOf(
                ReviewTemplate("Bạn đang kỷ luật tài chính rất tốt với {topHabit} ở {rate}%. Đừng tăng độ phức tạp — giữ hệ thống đơn giản, đều đặn.")
            ),
            TrendBucket.LOW to listOf(
                ReviewTemplate("Nhóm tài chính đang bị bỏ lỡ — {topHabit} dừng {missed} ngày. Một phiên 5 phút mỗi tối ghi chi tiêu nhanh sẽ dễ duy trì hơn."),
            ),
            TrendBucket.IMPROVING to listOf(
                ReviewTemplate("Tuần này bạn theo dõi tài chính đều hơn — {topHabit} đang vào nếp. Cố gắng giữ thói quen cùng giờ mỗi ngày.")
            )
        ),
        CategoryKind.RELATIONSHIPS to mapOf(
            TrendBucket.STABLE to listOf(
                ReviewTemplate("Quan hệ đang được nuôi dưỡng đều quanh {topHabit}. Một tin nhắn ngắn cho người thân mỗi tuần thường giá trị hơn ta nghĩ."),
                ReviewTemplate("Nhóm kết nối đang ở {rate}%. {topHabit} là bước đẹp — thử mở rộng đối tượng (gia đình, bạn cũ) để đa dạng nguồn năng lượng.")
            ),
            TrendBucket.HIGH to listOf(
                ReviewTemplate("Bạn đang đầu tư rất tốt cho quan hệ với chuỗi {streak} ngày quanh {topHabit}. Đây là khoản tích lũy rất quý.")
            ),
            TrendBucket.LOW to listOf(
                ReviewTemplate("{topHabit} bị lỡ {missed} ngày — kết nối thường bị bỏ qua khi bận. Một tin nhắn 30 giây vẫn đủ để giữ sợi dây.")
            ),
            TrendBucket.IMPROVING to listOf(
                ReviewTemplate("Tuần này bạn kết nối nhiều hơn tuần trước — {topHabit} đang trở lại. Giữ một khung giờ cố định để dễ duy trì.")
            )
        ),
        CategoryKind.OTHER to mapOf(
            TrendBucket.HIGH to listOf(
                ReviewTemplate("{topHabit} đang giữ rất tốt ở {rate}%. Hãy ưu tiên duy trì chứ chưa cần tăng thêm thử thách."),
                ReviewTemplate("Nhóm này đang vận hành ổn với chuỗi {streak} ngày — {topHabit} là điểm tựa đáng giữ.")
            ),
            TrendBucket.STABLE to listOf(
                ReviewTemplate("Nhóm đang ở {rate}%. {topHabit} có vẻ là chỗ ổn định nhất; thử bám vào đó trước khi mở rộng."),
                ReviewTemplate("{habitCount} thói quen đang vận hành đều — một thay đổi nhỏ ở {topHabit} có thể tạo cảm hứng mới.")
            ),
            TrendBucket.LOW to listOf(
                ReviewTemplate("Nhóm đang khó vào nhịp ({rate}%). {topHabit} có thể đang quá tham vọng — thử bản nhẹ hơn trong 1-2 tuần."),
                ReviewTemplate("Có dấu hiệu quá tải — {topHabit} bỏ lỡ {missed} ngày. Tạm dừng vài thói quen ít ưu tiên sẽ giúp tập trung.")
            ),
            TrendBucket.IMPROVING to listOf(
                ReviewTemplate("Tuần này tiến bộ hơn tuần trước — {topHabit} đang vào nhịp. Một bước nhỏ tiếp theo là vừa sức.")
            )
        )
    )

    // ── Suggestion pool ─────────────────────────────────────────
    // Each entry is a stand-alone SuggestedHabit. The map key (the
    // "_RECOVERY" suffix) is a string so we can stash an overlay pool
    // without polluting CategoryKind with a non-category value.

    /**
     * Recovery overlay swapped in when the user is OVERLOADED or
     * RECOVERY_NEEDING — kept as its own constant so the main pool stays
     * typed `Map<CategoryKind, …>` instead of `Map<Any, …>`.
     */
    private val RECOVERY_OVERLAY: List<SuggestedHabit> = listOf(
        SuggestedHabit("Một ngày nghỉ chủ động mỗi tuần", "🌿", "1 ngày không thêm thói quen mới.", "EASY", "Tránh kiệt sức", "Lịch trình bền hơn"),
        SuggestedHabit("Giảm 1 thói quen ít ưu tiên", "📉", "Tạm dừng thói quen ít quan trọng.", "EASY", "Tập trung vào điều quan trọng", "Tăng tỉ lệ hoàn thành"),
        SuggestedHabit("Giãn cơ nhẹ 5 phút trước ngủ", "🧘", "Giãn cơ chậm, không cố sức.", "EASY", "Phục hồi cơ thể", "Ngủ ngon hơn"),
        SuggestedHabit("Hít thở sâu 1 phút khi căng thẳng", "🌬️", "1 phút thở chậm khi thấy quá tải.", "EASY", "Giảm cortisol", "Phản ứng tốt hơn")
    )

    private val SUGGESTION_POOL: Map<CategoryKind, List<SuggestedHabit>> = mapOf(
        CategoryKind.FITNESS to listOf(
            SuggestedHabit("Khởi động 5 phút trước tập", "🌅", "Vài động tác nhẹ giúp giảm chấn thương.", "EASY", "Bảo vệ khớp và tăng hiệu quả buổi tập", "Lặp đều 21 ngày sẽ thành phản xạ"),
            SuggestedHabit("Giãn cơ 10 phút sau tập", "🧘", "Giãn nhẹ ngay sau khi buổi tập kết thúc.", "EASY", "Rút ngắn thời gian đau mỏi", "Đều đặn cải thiện độ linh hoạt"),
            SuggestedHabit("Đi bộ chậm 15 phút buổi tối", "🚶", "Vận động nhẹ giúp tiêu hóa và ngủ ngon.", "EASY", "Phục hồi mà vẫn duy trì nhịp", "Mỗi tối 15 phút = 100km/năm"),
            SuggestedHabit("1 ngày phục hồi mỗi tuần", "💆", "Một ngày không tập nặng, chỉ nghỉ ngơi và giãn cơ.", "EASY", "Tránh quá tải và chấn thương", "Lịch tập bền hơn"),
            SuggestedHabit("Uống 2 lít nước trong ngày tập", "💧", "Uống nước đủ trong và sau buổi tập.", "EASY", "Phục hồi nhanh hơn", "Tăng hiệu quả mọi buổi tập")
        ),
        CategoryKind.STUDY to listOf(
            SuggestedHabit("Ôn 5 phút trước khi ngủ", "📚", "Đọc lại bài học chính trong 5 phút.", "EASY", "Cải thiện trí nhớ dài hạn", "Đều mỗi đêm sẽ nhân đôi hiệu quả"),
            SuggestedHabit("Pomodoro 25 phút buổi sáng", "⏱️", "Một phiên tập trung 25 phút khi đầu óc tỉnh táo nhất.", "MEDIUM", "Tận dụng năng lượng buổi sáng", "Hình thành thói quen tập trung sâu"),
            SuggestedHabit("Ghi 3 điều học được mỗi tối", "📝", "Viết ngắn 3 điều bạn đã học hôm nay.", "EASY", "Củng cố kiến thức và tự nhận thức", "30 ngày = 90 ý tưởng"),
            SuggestedHabit("Đọc 10 trang sách trước 22:00", "📖", "Đọc trước khi điện thoại — não dễ tiếp thu hơn.", "EASY", "Giảm thời gian màn hình", "10 trang/ngày = 12-15 cuốn/năm"),
            SuggestedHabit("Tổng kết tuần 10 phút", "🗒️", "Cuối tuần viết những gì đã học và cần ôn.", "EASY", "Nhìn rõ tiến độ", "Tránh học lan man")
        ),
        CategoryKind.SLEEP to listOf(
            SuggestedHabit("Tắt thiết bị 30 phút trước khi ngủ", "📵", "Không màn hình trước giờ ngủ.", "MEDIUM", "Cải thiện chất lượng giấc ngủ", "1 tuần đã đủ thấy khác biệt"),
            SuggestedHabit("Giờ đi ngủ cố định", "🌙", "Lên giường cùng một khung giờ mỗi đêm.", "MEDIUM", "Ổn định đồng hồ sinh học", "Tỉnh táo hơn vào sáng"),
            SuggestedHabit("Đọc sách 10 phút trước ngủ", "📕", "Một cuốn sách giấy, ánh sáng ấm.", "EASY", "Não chuyển sang chế độ nghỉ tự nhiên", "Ngủ nhanh hơn"),
            SuggestedHabit("Tránh caffeine sau 14:00", "☕", "Cà phê / trà đặc trước 14:00.", "MEDIUM", "Giấc ngủ sâu hơn", "Ít trằn trọc giữa đêm"),
            SuggestedHabit("Phòng ngủ mát và tối", "🛏️", "Để phòng tối hoàn toàn, nhiệt độ 22-25°C.", "EASY", "Ngủ sâu hơn", "Cải thiện ngay đêm đầu")
        ),
        CategoryKind.MINDFULNESS to listOf(
            SuggestedHabit("Thở sâu 4-7-8 trước ngủ", "🌬️", "Hít 4 giây, giữ 7, thở 8 — 4 lần.", "EASY", "Giảm căng thẳng nhanh", "Vào giấc dễ hơn"),
            SuggestedHabit("Viết 3 điều biết ơn buổi tối", "🙏", "3 điều biết ơn ngắn trước khi ngủ.", "EASY", "Tâm trạng tích cực hơn", "Hiệu ứng tăng theo thời gian"),
            SuggestedHabit("5 phút yên lặng buổi sáng", "🌅", "Ngồi yên 5 phút trước khi chạm điện thoại.", "EASY", "Khởi đầu ngày bình thản", "Tinh thần ổn định cả ngày"),
            SuggestedHabit("Đặt tên cảm xúc 3 lần/ngày", "💭", "Mỗi lần thấy cảm xúc mạnh, đặt tên cho nó.", "EASY", "Tăng nhận thức cảm xúc", "Giảm phản ứng bốc đồng"),
            SuggestedHabit("Đi bộ trong im lặng 10 phút", "🚶‍♀️", "Đi bộ không nghe gì, chú ý hơi thở.", "EASY", "Tâm trí rõ ràng hơn", "Hiệu ứng giống thiền")
        ),
        CategoryKind.NUTRITION to listOf(
            SuggestedHabit("Uống 1 ly nước khi thức dậy", "💧", "Một ly nước trước khi làm gì khác.", "EASY", "Khởi động trao đổi chất", "Năng lượng đầu ngày tốt hơn"),
            SuggestedHabit("Bữa sáng có protein", "🥚", "Trứng, sữa, đậu — chọn 1 mỗi sáng.", "MEDIUM", "No lâu, ít thèm vặt", "Ổn định đường huyết"),
            SuggestedHabit("Một bữa rau xanh mỗi ngày", "🥗", "Ít nhất 1 bữa có rau xanh đậm.", "EASY", "Vi chất và chất xơ", "Tiêu hóa tốt hơn"),
            SuggestedHabit("Ăn chậm 20 phút/bữa", "🍴", "Ăn chậm, không màn hình.", "MEDIUM", "Tiêu hóa và no đúng lúc", "Ăn ít hơn tự nhiên"),
            SuggestedHabit("Không snack sau 21:00", "🚫", "Bếp đóng cửa sau 21:00.", "MEDIUM", "Giấc ngủ tốt hơn", "Cân nặng ổn định")
        ),
        CategoryKind.FINANCE to listOf(
            SuggestedHabit("Ghi chi tiêu 2 phút mỗi tối", "💸", "Ghi nhanh các khoản chi trong ngày.", "EASY", "Nhận thức chi tiêu", "Cuối tháng thấy rõ thói quen"),
            SuggestedHabit("Tự động chuyển 10% tiết kiệm", "🏦", "Ngay khi nhận thu nhập, chuyển 10% sang tiết kiệm.", "MEDIUM", "Tiết kiệm trước khi tiêu", "Quỹ dự phòng lớn dần"),
            SuggestedHabit("Xem lại sao kê mỗi tuần", "📊", "10 phút mỗi cuối tuần.", "EASY", "Bắt được chi tiêu lãng phí", "Kiểm soát tốt hơn theo tháng"),
            SuggestedHabit("Không mua impulse trong 24h", "⏳", "Đợi 24h trước khi mua thứ không thiết yếu.", "MEDIUM", "Giảm mua hối tiếc", "Tiết kiệm nhiều khoản"),
            SuggestedHabit("Mục tiêu tiết kiệm tháng", "🎯", "Đặt 1 mục tiêu nhỏ + theo dõi hàng tuần.", "MEDIUM", "Có hướng đi rõ", "Đạt được cảm giác chiến thắng")
        ),
        CategoryKind.RELATIONSHIPS to listOf(
            SuggestedHabit("Một tin nhắn cho người thân mỗi ngày", "💌", "1 dòng cho gia đình / bạn cũ.", "EASY", "Giữ kết nối nhẹ nhàng", "Tích lũy mối quan hệ"),
            SuggestedHabit("Gọi điện gia đình mỗi tuần", "📞", "1 cuộc gọi mỗi cuối tuần.", "EASY", "Tăng cảm giác thuộc về", "Sức khỏe tinh thần dài hạn"),
            SuggestedHabit("Hỏi 1 câu sâu hơn 'ổn không?'", "💬", "1 ngày 1 câu hỏi sâu cho người thân.", "EASY", "Quan hệ chất lượng hơn", "Nhận về nhiều hơn"),
            SuggestedHabit("Hẹn cà phê 1 lần/tuần", "☕", "1 buổi gặp gỡ trực tiếp mỗi tuần.", "MEDIUM", "Kết nối thật, không qua màn hình", "Tâm trạng tốt hơn"),
            SuggestedHabit("Cảm ơn ai đó mỗi ngày", "🙏", "1 lời cảm ơn cụ thể mỗi ngày.", "EASY", "Nuôi dưỡng quan hệ tích cực", "Vòng phản hồi tốt")
        ),
        CategoryKind.OTHER to listOf(
            SuggestedHabit("Một việc nhỏ quan trọng mỗi sáng", "🌅", "Chọn 1 việc quan trọng + làm trước.", "EASY", "Đà tích cực cả ngày", "21 ngày thành thói quen"),
            SuggestedHabit("Dọn bàn làm việc 2 phút", "🧹", "Trước khi đóng máy mỗi ngày.", "EASY", "Khởi đầu ngày sau gọn gàng", "Tâm trí nhẹ hơn"),
            SuggestedHabit("3 phút thở sâu trước họp", "🌬️", "Trước mỗi cuộc họp / công việc khó.", "EASY", "Bình tĩnh và tập trung hơn", "Phản ứng tốt hơn"),
            SuggestedHabit("Đánh giá ngày 1 phút", "📓", "Hôm nay đi tốt chỗ nào, cần đổi gì.", "EASY", "Học từ chính mình", "Tiến bộ rõ theo tuần"),
            SuggestedHabit("Cười với bản thân trong gương", "😊", "Mỗi sáng — 5 giây.", "EASY", "Tự ái và tâm trạng tốt", "Hiệu ứng nhanh")
        )
    )
}

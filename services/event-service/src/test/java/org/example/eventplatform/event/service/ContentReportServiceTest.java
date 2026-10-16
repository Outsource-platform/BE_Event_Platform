package org.example.eventplatform.event.service;

import org.example.eventplatform.event.dto.ReportDtos;
import org.example.eventplatform.event.entity.ChatMessage;
import org.example.eventplatform.event.entity.ContentReport;
import org.example.eventplatform.event.entity.Event;
import org.example.eventplatform.event.entity.ShowRating;
import org.example.eventplatform.event.repository.ChatMessageRepository;
import org.example.eventplatform.event.repository.ContentReportRepository;
import org.example.eventplatform.event.repository.EventRepository;
import org.example.eventplatform.event.repository.ShowRatingRepository;
import org.example.eventplatform.shared.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContentReportServiceTest {

    private final ContentReportRepository reports = mock(ContentReportRepository.class);
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final ShowRatingRepository ratings = mock(ShowRatingRepository.class);
    private final EventRepository events = mock(EventRepository.class);
    private ContentReportService service;

    @BeforeEach
    void setUp() {
        service = new ContentReportService(reports, messages, ratings, events);
        when(reports.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private static ReportDtos.CreateRequest req(String type, long id, String reason) {
        var r = new ReportDtos.CreateRequest();
        r.setTargetType(type);
        r.setTargetId(id);
        r.setReason(reason);
        r.setNote("  ");
        return r;
    }

    @Test
    void baoCaoTinNhanChupLaiNoiDung() {
        when(messages.findById(5L)).thenReturn(Optional.of(ChatMessage.builder().id(5L).content("lời lẽ xấu").build()));
        var item = service.create(9L, req("chat_message", 5, "offensive"));
        assertThat(item.targetType()).isEqualTo("CHAT_MESSAGE");
        assertThat(item.reason()).isEqualTo("OFFENSIVE");
        assertThat(item.snapshot()).isEqualTo("lời lẽ xấu");
        assertThat(item.note()).isNull();
        assertThat(item.status()).isEqualTo("OPEN");
    }

    @Test
    void baoCaoLaiKhiChuaXuLyThiKhongTaoThem() {
        var open = ContentReport.builder().id(1L).reporterUserId(9L).targetType("RATING").targetId(3L).reason("SPAM").build();
        when(reports.findFirstByReporterUserIdAndTargetTypeAndTargetIdAndStatus(9L, "RATING", 3L, "OPEN")).thenReturn(Optional.of(open));
        assertThat(service.create(9L, req("RATING", 3, "SPAM")).id()).isEqualTo(1L);
        verify(reports, never()).save(any());
    }

    @Test
    void loaiHoacLyDoSaiBiTuChoi() {
        assertThatThrownBy(() -> service.create(9L, req("USER", 1, "SPAM"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.create(9L, req("RATING", 1, "BORING"))).isInstanceOf(ApiException.class);
    }

    @Test
    void noiDungKhongTonTaiBiTuChoi() {
        when(ratings.findById(77L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(9L, req("RATING", 77, "SPAM"))).isInstanceOf(ApiException.class);
    }

    @Test
    void goDanhGiaGiuSoSaoBoLoiBinhVaDongCacBaoCaoCungNoiDung() {
        var rating = ShowRating.builder().id(3L).stars(1).comment("tệ hại").build();
        var a = ContentReport.builder().id(1L).reporterUserId(9L).targetType("RATING").targetId(3L).reason("SPAM").build();
        var b = ContentReport.builder().id(2L).reporterUserId(8L).targetType("RATING").targetId(3L).reason("OTHER").build();
        when(reports.findById(1L)).thenReturn(Optional.of(a));
        when(ratings.findById(3L)).thenReturn(Optional.of(rating));
        when(reports.findByTargetTypeAndTargetIdAndStatus("RATING", 3L, "OPEN")).thenReturn(List.of(a, b));
        var item = service.handle(1L, "removed");
        assertThat(item.status()).isEqualTo("REMOVED");
        assertThat(rating.getStars()).isEqualTo(1);
        assertThat(rating.getComment()).isNull();
        assertThat(b.getStatus()).isEqualTo("REMOVED");
        assertThat(b.getHandledAt()).isNotNull();
    }

    @Test
    void goShowKhoiKhamPhaVaGoTinNhan() {
        var event = new Event();
        event.setShowcasePublished(true);
        var msg = ChatMessage.builder().id(5L).content("x").imageUrl("https://a/b.webp").build();
        when(reports.findById(1L)).thenReturn(Optional.of(ContentReport.builder().id(1L).targetType("SHOWCASE").targetId(4L).build()));
        when(reports.findById(2L)).thenReturn(Optional.of(ContentReport.builder().id(2L).targetType("CHAT_MESSAGE").targetId(5L).build()));
        when(events.findById(4L)).thenReturn(Optional.of(event));
        when(messages.findById(5L)).thenReturn(Optional.of(msg));
        service.handle(1L, "REMOVED");
        service.handle(2L, "REMOVED");
        assertThat(event.getShowcasePublished()).isFalse();
        assertThat(msg.getContent()).isEqualTo(ContentReportService.REMOVED_MESSAGE);
        assertThat(msg.getImageUrl()).isNull();
    }

    @Test
    void boQuaKhongDungVaoNoiDung() {
        when(reports.findById(1L)).thenReturn(Optional.of(ContentReport.builder().id(1L).targetType("RATING").targetId(3L).build()));
        assertThat(service.handle(1L, "DISMISSED").status()).isEqualTo("DISMISSED");
        verify(ratings, never()).findById(any());
        assertThatThrownBy(() -> service.handle(1L, "DELETE")).isInstanceOf(ApiException.class);
    }
}

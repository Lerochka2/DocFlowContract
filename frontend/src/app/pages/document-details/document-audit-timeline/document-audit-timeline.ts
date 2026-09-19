import {
  Component,
  EventEmitter,
  Input,
  Output
} from '@angular/core';

import {
  DocumentAuditEventResponse
} from '../../../core/workspace/workspace.service';

interface AuditDetail {
  label: string;
  value: string;
}

@Component({
  selector: 'app-document-audit-timeline',
  standalone: true,
  imports: [],
  templateUrl: './document-audit-timeline.html',
  styleUrl: './document-audit-timeline.scss'
})
export class DocumentAuditTimelineComponent {

  @Input()
  events: DocumentAuditEventResponse[] = [];

  @Input()
  loading = false;

  @Input()
  errorMessage = '';

  @Output()
  retry = new EventEmitter<void>();


  getEventLabel(
    eventType: string
  ): string {

    switch (eventType) {

      case 'DOCUMENT_UPLOADED':
        return 'Документ загружен';

      case 'AI_ANALYSIS_STARTED':
        return 'AI-анализ запущен';

      case 'AI_ANALYSIS_COMPLETED':
        return 'AI-анализ завершён';

      case 'AI_ANALYSIS_FAILED':
        return 'Ошибка AI-анализа';

      case 'RISK_ANALYSIS_COMPLETED':
        return 'Анализ рисков';

      case 'CLASSIFICATION_CONFIRMED':
        return 'Классификация подтверждена';

      case 'CLASSIFICATION_CHANGED':
        return 'Классификация изменена';

      case 'METADATA_UPDATED':
        return 'Реквизиты изменены';

      case 'DOCUMENT_LINKED':
        return 'Привязка к договору';

      case 'SPECIALIST_APPROVED':
        return 'Документ согласован';

      case 'RETURNED_FOR_CORRECTION':
        return 'Возврат на доработку';

      case 'REJECTED':
        return 'Документ отклонён';

      case 'RESUBMITTED_FOR_REVIEW':
        return 'Повторно направлен на проверку';

      case 'COMMENT_ADDED':
        return 'Комментарий добавлен';

      case 'PREPARED_FOR_SED':
        return 'Подготовлен к СЭД';

      case 'SENT_TO_SED':
        return 'Отправлен в СЭД';

      case 'SED_SEND_FAILED':
        return 'Ошибка отправки в СЭД';

      case 'DOCUMENT_ARCHIVED':
        return 'Документ архивирован';

      case 'DOCUMENT_RESTORED':
        return 'Документ восстановлен';

      default:
        return eventType;
    }
  }


  getEventTone(
    eventType: string
  ): string {

    switch (eventType) {

      case 'AI_ANALYSIS_STARTED':
      case 'AI_ANALYSIS_COMPLETED':
      case 'RISK_ANALYSIS_COMPLETED':
        return 'ai';

      case 'CLASSIFICATION_CONFIRMED':
      case 'DOCUMENT_LINKED':
      case 'SPECIALIST_APPROVED':
      case 'PREPARED_FOR_SED':
      case 'SENT_TO_SED':
      case 'DOCUMENT_RESTORED':
        return 'success';

      case 'RETURNED_FOR_CORRECTION':
      case 'RESUBMITTED_FOR_REVIEW':
        return 'warning';

      case 'AI_ANALYSIS_FAILED':
      case 'REJECTED':
      case 'SED_SEND_FAILED':
        return 'danger';

      default:
        return 'neutral';
    }
  }


  getEventIcon(
    eventType: string
  ): string {

    switch (eventType) {

      case 'DOCUMENT_UPLOADED':
        return '↑';

      case 'AI_ANALYSIS_STARTED':
      case 'AI_ANALYSIS_COMPLETED':
      case 'RISK_ANALYSIS_COMPLETED':
        return 'AI';

      case 'CLASSIFICATION_CONFIRMED':
      case 'SPECIALIST_APPROVED':
        return '✓';

      case 'CLASSIFICATION_CHANGED':
        return '↔';

      case 'METADATA_UPDATED':
        return '✎';

      case 'DOCUMENT_LINKED':
        return '↗';

      case 'RETURNED_FOR_CORRECTION':
        return '↩';

      case 'RESUBMITTED_FOR_REVIEW':
        return '↻';

      case 'REJECTED':
      case 'AI_ANALYSIS_FAILED':
      case 'SED_SEND_FAILED':
        return '×';

      case 'COMMENT_ADDED':
        return '…';

      case 'PREPARED_FOR_SED':
        return 'S';

      case 'SENT_TO_SED':
        return '→';

      default:
        return '•';
    }
  }


  getActorTypeLabel(
    event: DocumentAuditEventResponse
  ): string {

    return event.actorType === 'SYSTEM'
      ? 'Система'
      : 'Пользователь';
  }


  formatDateTime(
    value: string | null
  ): string {

    if (!value) {
      return '—';
    }

    const date =
      new Date(value);

    if (
      Number.isNaN(
        date.getTime()
      )
    ) {
      return value;
    }

    return new Intl.DateTimeFormat(
      'ru-RU',
      {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      }
    ).format(
      date
    );
  }


  getPrimaryDetails(
    event: DocumentAuditEventResponse
  ): AuditDetail[] {

    return this.parseDetails(
      event.details,
      event.eventType
    ).filter(
      detail =>
        !this.isTechnicalDetail(
          detail.label
        )
    );
  }


  getTechnicalDetails(
    event: DocumentAuditEventResponse
  ): AuditDetail[] {

    return this.parseDetails(
      event.details,
      event.eventType
    ).filter(
      detail =>
        this.isTechnicalDetail(
          detail.label
        )
        &&
        detail.label !== 'Анализатор'
    );
  }


  private parseDetails(
    details: string | null,
    eventType: string
  ): AuditDetail[] {

    if (!details) {
      return [];
    }

    const detailLines =
      eventType === 'RISK_ANALYSIS_COMPLETED'
        ? details.split(/\r?\n|;\s*/)
        : details.split(/\r?\n/);

    return detailLines
      .map(
        line => line.trim()
      )
      .filter(
        line => Boolean(line)
      )
      .map(
        line => {

          const separatorIndex =
            line.indexOf(':');

          if (separatorIndex < 0) {

            return {
              label: 'Комментарий',
              value: line
            };
          }

          const rawLabel =
            line
              .slice(
                0,
                separatorIndex
              )
              .trim();

          const rawValue =
            line
              .slice(
                separatorIndex + 1
              )
              .trim();

          const label =
            this.normalizeDetailLabel(
              rawLabel
            );

          return {
            label,
            value:
              this.normalizeDetailValue(
                label,
                rawValue
              )
          };
        }
      );
  }


  private normalizeDetailLabel(
    label: string
  ): string {

    switch (label) {

      case 'Имя файла':
        return 'Файл';

      case 'Предсказанный тип':
        return 'Тип AI';

      case 'Итоговый тип AI':
        return 'Итоговый тип';

      case 'Классификация принята автоматически':
        return 'Автопринятие';

      case 'Требуется ручная проверка':
        return 'Ручная проверка';

      case 'Версия модели':
        return 'Модель';

      case 'Статус перед анализом':
        return 'Статус до анализа';

      case 'индекс риска':
        return 'Индекс риска';

      case 'найдено условий':
        return 'Найдено условий';

      case 'анализатор':
        return 'Анализатор';

      default:
        return label;
    }
  }


  private normalizeDetailValue(
    label: string,
    value: string
  ): string {

    if (value === 'true') {
      return 'Да';
    }

    if (value === 'false') {
      return 'Нет';
    }

    if (label === 'Уровень риска') {

      switch (value) {

        case 'HIGH':
          return 'Высокий риск';

        case 'MEDIUM':
          return 'Средний риск';

        case 'LOW':
          return 'Низкий риск';
      }
    }

    const documentType =
      this.getDocumentTypeLabel(
        value
      );

    if (documentType !== null) {
      return documentType;
    }

    const status =
      this.getStatusLabel(
        value
      );

    if (status !== null) {
      return status;
    }

    if (
      label === 'Уверенность'
      &&
      /^\d+([.,]\d+)?$/.test(value)
    ) {

      const numeric =
        Number(
          value.replace(',', '.')
        );

      if (!Number.isNaN(numeric)) {

        const percent =
          numeric <= 1
            ? numeric * 100
            : numeric;

        return `${percent.toFixed(2).replace('.', ',')}%`;
      }
    }

    if (
      label === 'Размер'
    ) {

      const match =
        /^(\d+)\s*байт$/i.exec(
          value
        );

      if (match) {

        const bytes =
          Number(
            match[1]
          );

        if (bytes >= 1024) {

          return `${(
            bytes / 1024
          ).toFixed(1).replace('.', ',')} КБ`;
        }
      }
    }

    return value;
  }


  private getDocumentTypeLabel(
    value: string
  ): string | null {

    switch (value) {

      case 'CONTRACT':
        return 'Договор';

      case 'APPENDIX':
        return 'Приложение';

      case 'AMENDMENT_CHANGE':
        return 'Доп. соглашение — изменение';

      case 'AMENDMENT_PROLONGATION':
        return 'Доп. соглашение — продление';

      default:
        return null;
    }
  }


  private getStatusLabel(
    value: string
  ): string | null {

    switch (value) {

      case 'UPLOADED':
        return 'Загружен';

      case 'AI_PROCESSING':
        return 'AI-анализ';

      case 'REVIEW_REQUIRED':
        return 'Требует проверки';

      case 'LINK_REQUIRED':
        return 'Требует привязки';

      case 'LINKED':
        return 'Связан с договором';

      case 'VERIFIED':
        return 'Проверен';

      case 'APPROVED':
        return 'Согласован';

      case 'RETURNED_FOR_CORRECTION':
        return 'На доработке';

      case 'REJECTED':
        return 'Отклонён';

      case 'READY_FOR_SED':
        return 'Готов к отправке';

      case 'SENT_TO_SED':
        return 'Отправлен в СЭД';

      case 'PROCESSING_FAILED':
        return 'Ошибка обработки';

      case 'SUCCESS':
        return 'Успешно';

      default:
        return null;
    }
  }


  private isTechnicalDetail(
    label: string
  ): boolean {

    return (
      label === 'SHA-256'
      ||
      label === 'Анализатор'
      ||
      label === 'Модель'
      ||
      label === 'OCR'
      ||
      label === 'Автопринятие'
      ||
      label === 'Статус до анализа'
      ||
      label === 'Статус обработки'
    );
  }
}
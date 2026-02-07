# Microcks Custom

Кастомная версия Microcks для локального развития и тестирования API.

## Быстрый старт

```bash
# Запуск
docker-compose up -d

# Доступ: http://localhost:8080

# Остановка
docker-compose down
```

## Структура

```
microcks-custom/
├── docker-compose.yml  # Локальная разработка
└── README.md
```

## Связанные репозитории

- **Helm Charts**: https://github.com/sergeimeshe2-spec/microcks-helm
- **Docker**: https://github.com/sergeimeshe2-spec/microcks-docker
- **CI/CD**: https://github.com/sergeimeshe2-spec/microcks-ci
- **API Specs**: https://github.com/sergeimeshe2-spec/microcks-specs

## Конфигурация

- **OAuth2**: отключен
- **PostgreSQL**: `microcks` / `microcks`
- **MongoDB**: для результатов тестов

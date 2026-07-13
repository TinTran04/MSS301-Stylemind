# PROJECT SNAPSHOT FOR ARCHITECT REVIEW
**Generated on:** 1783697810.4237697
**Project Root:** c:\Users\KHAI\Documents\semester 8\MSS301-Code\MSS301-Stylemind
---

## 1. CẤU TRÚC CÂY THƯ MỤC (FOLDER TREE)

```
MSS301-Stylemind/
├── .devin
│   └── workflows
├── .gitignore
├── AGENTS.md
├── BE
│   ├── .env
│   ├── .env.example
│   ├── .env.template
│   ├── .env.tested
│   ├── .gitignore
│   ├── .gitkeep
│   ├── AGENTS.md
│   ├── ai-agent-service
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src
│   │       └── main
│   │           ├── java
│   │           │   └── com
│   │           │       └── stylemind
│   │           │           └── ai
│   │           │               ├── AiAgentServiceApplication.java
│   │           │               ├── controller
│   │           │               │   ├── AdminAiIndexJobController.java
│   │           │               │   └── AiStylistController.java
│   │           │               ├── dto
│   │           │               │   ├── BundleResponse.java
│   │           │               │   ├── ChatRequest.java
│   │           │               │   ├── ChatResponse.java
│   │           │               │   ├── IndexJobRequest.java
│   │           │               │   ├── IndexJobResponse.java
│   │           │               │   └── RecommendedProduct.java
│   │           │               ├── entity
│   │           │               │   ├── AiCuratedBundle.java
│   │           │               │   ├── AiCuratedBundleItem.java
│   │           │               │   ├── AiIndexJob.java
│   │           │               │   ├── ChatMessage.java
│   │           │               │   └── ChatSession.java
│   │           │               ├── feign
│   │           │               │   └── ProductClient.java
│   │           │               ├── repository
│   │           │               │   ├── AiCuratedBundleItemRepository.java
│   │           │               │   ├── AiCuratedBundleRepository.java
│   │           │               │   ├── AiIndexJobRepository.java
│   │           │               │   ├── ChatMessageRepository.java
│   │           │               │   └── ChatSessionRepository.java
│   │           │               └── service
│   │           │                   ├── AiChatService.java
│   │           │                   └── AiIndexJobService.java
│   │           └── resources
│   │               ├── application-docker.yml
│   │               ├── application-local.yml
│   │               └── application.yml
│   ├── api-gateway
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src
│   │       ├── main
│   │       │   ├── java
│   │       │   │   └── com
│   │       │   │       └── stylemind
│   │       │   │           └── gateway
│   │       │   │               ├── ApiGatewayApplication.java
│   │       │   │               ├── config
│   │       │   │               │   └── SecurityConfig.java
│   │       │   │               ├── filter
│   │       │   │               │   ├── JwtAuthenticationFilter.java
│   │       │   │               │   └── RateLimitFilter.java
│   │       │   │               └── security
│   │       │   │                   ├── JwtUtil.java
│   │       │   │                   └── UserPrincipal.java
│   │       │   └── resources
│   │       │       ├── application-docker.yml
│   │       │       ├── application-local.yml
│   │       │       └── application.yml
│   │       └── test
│   │           └── java
│   │               └── com
│   │                   └── stylemind
│   │                       └── gateway
│   │                           └── filter
│   │                               └── JwtAuthenticationFilterTest.java
│   ├── auth-service
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src
│   │       ├── main
│   │       │   ├── java
│   │       │   │   └── com
│   │       │   │       └── stylemind
│   │       │   │           └── auth
│   │       │   │               ├── AuthServiceApplication.java
│   │       │   │               ├── controller
│   │       │   │               │   ├── AdminUserController.java
│   │       │   │               │   ├── AuthController.java
│   │       │   │               │   └── InternalUserController.java
│   │       │   │               ├── dto
│   │       │   │               │   ├── AdminCreateUserRequest.java
│   │       │   │               │   ├── AdminUserResponse.java
│   │       │   │               │   ├── AdminUserSummaryResponse.java
│   │       │   │               │   ├── AuthResponse.java
│   │       │   │               │   ├── ChangeEnabledRequest.java
│   │       │   │               │   ├── ChangeRoleRequest.java
│   │       │   │               │   ├── ForgotPasswordRequest.java
│   │       │   │               │   ├── ForgotPasswordVerifyResponse.java
│   │       │   │               │   ├── InternalEmailNotificationRequest.java
│   │       │   │               │   ├── InternalUserEmailResponse.java
│   │       │   │               │   ├── LoginRequest.java
│   │       │   │               │   ├── PasswordSetupRequest.java
│   │       │   │               │   ├── RegisterRequest.java
│   │       │   │               │   ├── ResendRegisterOtpRequest.java
│   │       │   │               │   ├── ResetForgotPasswordRequest.java
│   │       │   │               │   ├── UserResponse.java
│   │       │   │               │   ├── VerifyForgotPasswordOtpRequest.java
│   │       │   │               │   └── VerifyRegisterOtpRequest.java
│   │       │   │               ├── entity
│   │       │   │               │   ├── AccountStatus.java
│   │       │   │               │   ├── AuditLog.java
│   │       │   │               │   ├── PendingRegistration.java
│   │       │   │               │   └── User.java
│   │       │   │               ├── feign
│   │       │   │               │   └── NotificationInternalClient.java
│   │       │   │               ├── mapper
│   │       │   │               │   └── AuthMapper.java
│   │       │   │               ├── repository
│   │       │   │               │   ├── AuditLogRepository.java
│   │       │   │               │   ├── PendingRegistrationRepository.java
│   │       │   │               │   └── UserRepository.java
│   │       │   │               └── service
│   │       │   │                   └── AuthService.java
│   │       │   └── resources
│   │       │       ├── application-docker.yml
│   │       │       ├── application-local.yml
│   │       │       ├── application.yml
│   │       │       └── db
│   │       │           └── migration
│   │       │               ├── V1__baseline_auth_schema.sql
│   │       │               ├── V2__identity_boundary.sql
│   │       │               ├── V3__admin_account_audit_log.sql
│   │       │               └── V4__pending_registration.sql
│   │       └── test
│   │           ├── java
│   │           │   └── com
│   │           │       └── stylemind
│   │           │           └── auth
│   │           │               ├── dto
│   │           │               │   └── InternalEmailNotificationRequestTest.java
│   │           │               ├── mapper
│   │           │               │   └── AuthMapperTest.java
│   │           │               └── service
│   │           │                   └── AuthServiceTest.java
│   │           └── resources
│   │               └── mockito-extensions
│   │                   └── org.mockito.plugins.MockMaker
│   ├── cart-service
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src
│   │       ├── main
│   │       │   ├── java
│   │       │   │   └── com
│   │       │   │       └── stylemind
│   │       │   │           └── cart
│   │       │   │               ├── CartServiceApplication.java
│   │       │   │               ├── config
│   │       │   │               │   └── CustomUserDetailsService.java
│   │       │   │               ├── controller
│   │       │   │               │   ├── CartController.java
│   │       │   │               │   └── InternalCartController.java
│   │       │   │               ├── dto
│   │       │   │               │   ├── CartItemRequest.java
│   │       │   │               │   ├── CartItemResponse.java
│   │       │   │               │   ├── CartMergeRequest.java
│   │       │   │               │   └── CartResponse.java
│   │       │   │               ├── entity
│   │       │   │               │   ├── CartItem.java
│   │       │   │               │   └── ShoppingCart.java
│   │       │   │               ├── feign
│   │       │   │               │   └── ProductClient.java
│   │       │   │               ├── repository
│   │       │   │               │   ├── CartItemRepository.java
│   │       │   │               │   └── ShoppingCartRepository.java
│   │       │   │               └── service
│   │       │   │                   └── CartService.java
│   │       │   └── resources
│   │       │       ├── application-docker.yml
│   │       │       ├── application-local.yml
│   │       │       └── application.yml
│   │       └── test
│   │           └── java
│   │               └── com
│   │                   └── stylemind
│   │                       └── cart
│   │                           ├── controller
│   │                           │   └── CartControllerTest.java
│   │                           └── service
│   │                               └── CartServiceTest.java
│   ├── common-lib
│   │   ├── pom.xml
│   │   └── src
│   │       ├── main
│   │       │   └── java
│   │       │       └── com
│   │       │           └── stylemind
│   │       │               └── common
│   │       │                   ├── config
│   │       │                   │   ├── AuthenticationManagerConfig.java
│   │       │                   │   ├── JpaAuditingConfig.java
│   │       │                   │   ├── JwtAutoConfiguration.java
│   │       │                   │   ├── JwtKeyProperties.java
│   │       │                   │   ├── OpenApiConfig.java
│   │       │                   │   └── SecurityConfig.java
│   │       │                   ├── constant
│   │       │                   │   └── ErrorCode.java
│   │       │                   ├── dto
│   │       │                   │   ├── ApiResponse.java
│   │       │                   │   └── PageResponse.java
│   │       │                   ├── exception
│   │       │                   │   ├── BusinessException.java
│   │       │                   │   ├── CryptoException.java
│   │       │                   │   ├── GlobalExceptionHandler.java
│   │       │                   │   ├── InvalidKeyFormatException.java
│   │       │                   │   ├── KeyDecodingException.java
│   │       │                   │   └── KeyLoadException.java
│   │       │                   ├── feign
│   │       │                   │   └── FeignClientConfig.java
│   │       │                   ├── logging
│   │       │                   │   └── CorrelationIdFilter.java
│   │       │                   ├── security
│   │       │                   │   ├── CustomUserDetailsService.java
│   │       │                   │   ├── HeaderAuthenticationFilter.java
│   │       │                   │   ├── InternalAuthFilter.java
│   │       │                   │   ├── JwtAuthenticationFilter.java
│   │       │                   │   ├── JwtUtil.java
│   │       │                   │   ├── RsaKeyLoader.java
│   │       │                   │   └── UserPrincipal.java
│   │       │                   └── util
│   │       │                       ├── BaseEntity.java
│   │       │                       └── StringUtil.java
│   │       └── test
│   │           ├── java
│   │           │   └── com
│   │           │       └── stylemind
│   │           │           └── common
│   │           │               ├── config
│   │           │               │   └── SecurityConfigTest.java
│   │           │               └── security
│   │           │                   ├── HeaderAuthenticationFilterTest.java
│   │           │                   ├── JwtUtilTest.java
│   │           │                   └── RsaKeyLoaderTest.java
│   │           └── resources
│   │               └── mockito-extensions
│   │                   └── org.mockito.plugins.MockMaker
│   ├── docker-compose.infra.yml
│   ├── docker-compose.yml
│   ├── docs
│   │   ├── adoption
│   │   │   └── clean-clone-walkthrough.md
│   │   ├── agents
│   │   │   ├── claude-code.md
│   │   │   ├── codex.md
│   │   │   └── cursor.md
│   │   ├── ARCHITECTURE.md
│   │   ├── ARCHITECTURE_AND_QUALITY_REVIEW.md
│   │   ├── COMMAND_COOKBOOK.md
│   │   ├── CONTEXT_RULES.md
│   │   ├── decisions
│   │   │   ├── 0001-harness-first-development.md
│   │   │   ├── 0002-post-spec-product-lifecycle.md
│   │   │   ├── 0003-generic-spec-intake-harness.md
│   │   │   ├── 0004-sqlite-durable-layer.md
│   │   │   ├── 0005-prebuilt-rust-harness-cli.md
│   │   │   ├── 0006-phase-4-benchmark-triage.md
│   │   │   ├── 0007-hi-os-verification-gate.md
│   │   │   ├── 0008-canonical-public-release-origin.md
│   │   │   ├── 0009-release-verification-evidence-storage.md
│   │   │   ├── 0010-mcp-artifact-contracts.md
│   │   │   ├── 0011-harness-friction-taxonomy.md
│   │   │   ├── 0012-governance-report-schema.md
│   │   │   ├── 0013-hi-os-sovereign-identity.md
│   │   │   ├── 0014-production-clean-distribution-boundary.md
│   │   │   └── README.md
│   │   ├── examples
│   │   │   └── full-agent-workflow.md
│   │   ├── FEATURE_INTAKE.md
│   │   ├── FRICTION_TAXONOMY.md
│   │   ├── GLOSSARY.md
│   │   ├── GOVERNANCE_REPORT.md
│   │   ├── HARNESS.md
│   │   ├── HARNESS_BACKLOG.md
│   │   ├── HARNESS_COMPONENTS.md
│   │   ├── HARNESS_MATURITY.md
│   │   ├── local-development.md
│   │   ├── product
│   │   │   └── README.md
│   │   ├── README.md
│   │   ├── schemas
│   │   │   ├── codegraph-impact.schema.json
│   │   │   ├── context-ingest-result.schema.json
│   │   │   ├── friction-event.schema.json
│   │   │   ├── governance-report.schema.json
│   │   │   └── notebooklm-brief.schema.json
│   │   ├── stories
│   │   │   ├── backlog.md
│   │   │   └── README.md
│   │   ├── templates
│   │   │   ├── decision.md
│   │   │   ├── high-risk-story
│   │   │   │   ├── design.md
│   │   │   │   ├── execplan.md
│   │   │   │   ├── overview.md
│   │   │   │   └── validation.md
│   │   │   ├── spec-intake.md
│   │   │   ├── story.md
│   │   │   └── validation-report.md
│   │   ├── TEST_MATRIX.md
│   │   ├── TRACE_SPEC.md
│   │   └── troubleshooting.md
│   ├── harness-architecture.toml
│   ├── harness-release.toml
│   ├── hios.toml
│   ├── init-scripts
│   │   ├── 00-create-databases.sh.backup
│   │   ├── 01-auth-db.sql
│   │   ├── 02-user-db.sql
│   │   ├── 03-product-db.sql
│   │   ├── 04-product-seed-normalized-from-3-datasets.sql
│   │   ├── 05-cart-db.sql
│   │   ├── 06-order-db.sql
│   │   ├── 07-payment-db.sql
│   │   ├── 08-ai-db.sql
│   │   ├── 09-notification-db.sql
│   │   └── manual-patches
│   │       └── 2026-07-11-init-script-nullability-sync.sql
│   ├── Makefile
│   ├── notification-service
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src
│   │       ├── main
│   │       │   ├── java
│   │       │   │   └── com
│   │       │   │       └── stylemind
│   │       │   │           └── notification
│   │       │   │               ├── controller
│   │       │   │               │   ├── AdminNotificationController.java
│   │       │   │               │   ├── InternalNotificationController.java
│   │       │   │               │   └── NotificationController.java
│   │       │   │               ├── dto
│   │       │   │               │   ├── AdminNotificationSummaryResponse.java
│   │       │   │               │   ├── InternalEmailRequest.java
│   │       │   │               │   ├── NotificationRequest.java
│   │       │   │               │   └── NotificationResponse.java
│   │       │   │               ├── entity
│   │       │   │               │   └── NotificationLog.java
│   │       │   │               ├── NotificationServiceApplication.java
│   │       │   │               ├── repository
│   │       │   │               │   └── NotificationLogRepository.java
│   │       │   │               └── service
│   │       │   │                   └── NotificationService.java
│   │       │   └── resources
│   │       │       ├── application-docker.yml
│   │       │       ├── application-local.yml
│   │       │       └── application.yml
│   │       └── test
│   │           └── java
│   │               └── com
│   │                   └── stylemind
│   │                       └── notification
│   │                           └── service
│   │                               └── NotificationServiceTest.java
│   ├── order-service
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src
│   │       ├── main
│   │       │   ├── java
│   │       │   │   └── com
│   │       │   │       └── stylemind
│   │       │   │           └── order
│   │       │   │               ├── config
│   │       │   │               │   ├── JwtAuthFilter.java
│   │       │   │               │   └── ResilientReadFeignConfig.java
│   │       │   │               ├── controller
│   │       │   │               │   ├── AdminOrderController.java
│   │       │   │               │   ├── InternalOrderController.java
│   │       │   │               │   └── OrderController.java
│   │       │   │               ├── dto
│   │       │   │               │   ├── AdminOrderSummaryResponse.java
│   │       │   │               │   ├── CreateOrderRequest.java
│   │       │   │               │   ├── OrderItemRequest.java
│   │       │   │               │   ├── OrderItemResponse.java
│   │       │   │               │   ├── OrderResponse.java
│   │       │   │               │   └── UpdateOrderStatusRequest.java
│   │       │   │               ├── entity
│   │       │   │               │   ├── CheckoutIdempotency.java
│   │       │   │               │   ├── Order.java
│   │       │   │               │   ├── OrderItem.java
│   │       │   │               │   ├── OrderStatus.java
│   │       │   │               │   └── OrderStatusAuditLog.java
│   │       │   │               ├── exception
│   │       │   │               │   └── InvalidOrderStatusTransitionException.java
│   │       │   │               ├── feign
│   │       │   │               │   ├── CartClient.java
│   │       │   │               │   ├── NotificationClient.java
│   │       │   │               │   ├── PaymentClient.java
│   │       │   │               │   ├── ProductClient.java
│   │       │   │               │   └── UserClient.java
│   │       │   │               ├── job
│   │       │   │               │   └── OrderTimeoutJob.java
│   │       │   │               ├── OrderServiceApplication.java
│   │       │   │               ├── repository
│   │       │   │               │   ├── CheckoutIdempotencyRepository.java
│   │       │   │               │   ├── OrderItemRepository.java
│   │       │   │               │   ├── OrderRepository.java
│   │       │   │               │   └── OrderStatusAuditLogRepository.java
│   │       │   │               └── service
│   │       │   │                   ├── OrderService.java
│   │       │   │                   └── OrderStatusService.java
│   │       │   └── resources
│   │       │       ├── application-docker.yml
│   │       │       ├── application-local.yml
│   │       │       └── application.yml
│   │       └── test
│   │           └── java
│   │               └── com
│   │                   └── stylemind
│   │                       └── order
│   │                           ├── entity
│   │                           │   └── OrderStatusTest.java
│   │                           ├── job
│   │                           │   └── OrderTimeoutJobTest.java
│   │                           └── service
│   │                               ├── OrderServiceTest.java
│   │                               └── OrderStatusServiceTest.java
│   ├── payment-service
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src
│   │       ├── main
│   │       │   ├── java
│   │       │   │   └── com
│   │       │   │       └── stylemind
│   │       │   │           └── payment
│   │       │   │               ├── controller
│   │       │   │               │   ├── InternalPaymentController.java
│   │       │   │               │   └── SepayWebhookController.java
│   │       │   │               ├── dto
│   │       │   │               │   ├── CodCheckoutRequest.java
│   │       │   │               │   ├── PaymentResponse.java
│   │       │   │               │   ├── SepayCheckoutRequest.java
│   │       │   │               │   └── SepayWebhookPayload.java
│   │       │   │               ├── entity
│   │       │   │               │   ├── PaymentWebhookEvent.java
│   │       │   │               │   └── Transaction.java
│   │       │   │               ├── feign
│   │       │   │               │   └── OrderClient.java
│   │       │   │               ├── PaymentServiceApplication.java
│   │       │   │               ├── repository
│   │       │   │               │   ├── PaymentWebhookEventRepository.java
│   │       │   │               │   └── TransactionRepository.java
│   │       │   │               └── service
│   │       │   │                   ├── PaymentReferenceMatcher.java
│   │       │   │                   └── PaymentService.java
│   │       │   └── resources
│   │       │       ├── application-docker.yml
│   │       │       ├── application-local.yml
│   │       │       └── application.yml
│   │       └── test
│   │           └── java
│   │               └── com
│   │                   └── stylemind
│   │                       └── payment
│   │                           └── service
│   │                               ├── PaymentReferenceMatcherTest.java
│   │                               └── PaymentServiceTest.java
│   ├── pom.xml
│   ├── product-service
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src
│   │       ├── main
│   │       │   ├── java
│   │       │   │   └── com
│   │       │   │       └── stylemind
│   │       │   │           └── product
│   │       │   │               ├── config
│   │       │   │               │   └── CloudinaryConfig.java
│   │       │   │               ├── controller
│   │       │   │               │   ├── AdminCategoryController.java
│   │       │   │               │   ├── AdminProductController.java
│   │       │   │               │   ├── CategoryController.java
│   │       │   │               │   ├── InternalProductController.java
│   │       │   │               │   └── PublicProductController.java
│   │       │   │               ├── dto
│   │       │   │               │   ├── AdminProductSummaryResponse.java
│   │       │   │               │   ├── CategoryRequest.java
│   │       │   │               │   ├── CategoryResponse.java
│   │       │   │               │   ├── CategorySummaryResponse.java
│   │       │   │               │   ├── ProductImageResponse.java
│   │       │   │               │   ├── ProductRequest.java
│   │       │   │               │   ├── ProductResponse.java
│   │       │   │               │   ├── ProductVariantRequest.java
│   │       │   │               │   ├── ProductVariantResponse.java
│   │       │   │               │   ├── StatusUpdateRequest.java
│   │       │   │               │   └── VariantSnapshotResponse.java
│   │       │   │               ├── entity
│   │       │   │               │   ├── Category.java
│   │       │   │               │   ├── Product.java
│   │       │   │               │   ├── ProductAuditLog.java
│   │       │   │               │   ├── ProductCategory.java
│   │       │   │               │   ├── ProductCategoryId.java
│   │       │   │               │   ├── ProductImage.java
│   │       │   │               │   ├── ProductVariant.java
│   │       │   │               │   └── TargetDemographic.java
│   │       │   │               ├── ProductServiceApplication.java
│   │       │   │               ├── repository
│   │       │   │               │   ├── CategoryRepository.java
│   │       │   │               │   ├── ProductAuditLogRepository.java
│   │       │   │               │   ├── ProductCategoryRepository.java
│   │       │   │               │   ├── ProductImageRepository.java
│   │       │   │               │   ├── ProductRepository.java
│   │       │   │               │   └── ProductVariantRepository.java
│   │       │   │               └── service
│   │       │   │                   ├── CategoryService.java
│   │       │   │                   ├── image
│   │       │   │                   │   ├── CloudinaryProductImageStorage.java
│   │       │   │                   │   ├── ProductImageStorage.java
│   │       │   │                   │   └── StoredProductImage.java
│   │       │   │                   └── ProductService.java
│   │       │   └── resources
│   │       │       ├── application-docker.yml
│   │       │       ├── application-local.yml
│   │       │       ├── application.yml
│   │       │       └── db
│   │       │           └── migration
│   │       │               ├── V1__baseline_product_schema.sql
│   │       │               ├── V2__product_image_public_id.sql
│   │       │               ├── V3__product_variant_stock.sql
│   │       │               ├── V4__product_categories_and_english_demographic.sql
│   │       │               └── V5__product_images_updated_at.sql
│   │       └── test
│   │           └── java
│   │               └── com
│   │                   └── stylemind
│   │                       └── product
│   │                           ├── dto
│   │                           │   └── ProductVariantRequestTest.java
│   │                           └── service
│   │                               ├── CategoryServiceTest.java
│   │                               ├── image
│   │                               │   └── CloudinaryProductImageStorageTest.java
│   │                               └── ProductServiceTest.java
│   ├── PROJECT_SPEC.md
│   ├── README.harness.md
│   ├── README.md
│   ├── scripts
│   │   ├── migrations
│   │   │   └── migrate-auth-full-name-to-user-profile.sh
│   │   ├── README.md
│   │   ├── schema
│   │   │   ├── 001-init.sql
│   │   │   ├── 002-story-verify.sql
│   │   │   ├── 003-hi-os.sql
│   │   │   ├── 004-verification-gate.sql
│   │   │   ├── 005-release-verification.sql
│   │   │   ├── 006-context-ingest.sql
│   │   │   └── 007-friction-events.sql
│   │   ├── verify-adoption-docs.py
│   │   ├── verify-api-versioning.py
│   │   ├── verify-friction-taxonomy.py
│   │   ├── verify-governance-report-schema.py
│   │   └── verify-mcp-artifact-contracts.py
│   └── user-service
│       ├── Dockerfile
│       ├── pom.xml
│       └── src
│           ├── main
│           │   ├── java
│           │   │   └── com
│           │   │       └── stylemind
│           │   │           └── user
│           │   │               ├── controller
│           │   │               │   ├── InternalUserController.java
│           │   │               │   └── UserProfileController.java
│           │   │               ├── dto
│           │   │               │   ├── DeliveryAddressRequest.java
│           │   │               │   ├── DeliveryAddressResponse.java
│           │   │               │   ├── StyleProfileRequest.java
│           │   │               │   └── StyleProfileResponse.java
│           │   │               ├── entity
│           │   │               │   ├── CustomerStyleProfile.java
│           │   │               │   └── DeliveryAddress.java
│           │   │               ├── repository
│           │   │               │   ├── CustomerStyleProfileRepository.java
│           │   │               │   └── DeliveryAddressRepository.java
│           │   │               ├── service
│           │   │               │   └── UserProfileService.java
│           │   │               ├── UserServiceApplication.java
│           │   │               └── validation
│           │   │                   ├── JsonValidator.java
│           │   │                   └── ValidJson.java
│           │   └── resources
│           │       ├── application-docker.yml
│           │       ├── application-local.yml
│           │       ├── application.yml
│           │       └── db
│           │           └── migration
│           │               ├── V1__baseline_user_schema.sql
│           │               └── V2__profile_boundary.sql
│           └── test
│               ├── java
│               │   └── com
│               │       └── stylemind
│               │           └── user
│               │               └── service
│               │                   └── UserProfileServiceTest.java
│               └── resources
│                   └── mockito-extensions
│                       └── org.mockito.plugins.MockMaker
├── DOCKER_DEBUG_GUIDE.md
├── docs
│   ├── AGENT_WORKSPACE
│   │   ├── ARCHITECTURE_ANALYSIS.md
│   │   ├── ASYMMETRIC_JWT_IMPLEMENTATION_PLAN.md
│   │   ├── ASYMMETRIC_JWT_JAVA_REFACTORING_BLUEPRINT.md
│   │   ├── ASYMMETRIC_KEY_JWT_CONFIG_BLUEPRINT.md
│   │   ├── IMPLEMENTATION_LOG.md
│   │   ├── MEMORY
│   │   │   ├── CURRENT_STATE.md
│   │   │   └── DECISIONS.md
│   │   ├── README.md
│   │   ├── SERVICE_SEPARATION_PLAN.md
│   │   └── SOURCE_CODE_ISSUES.md
│   ├── api
│   │   └── 01-api-catalog.md
│   ├── architecture
│   │   ├── 01-system-architecture.md
│   │   ├── 02-api-conventions.md
│   │   ├── 03-auth-user-boundary.md
│   │   ├── 04-order-state-machine.md
│   │   ├── 05-checkout-saga.md
│   │   └── 06-security.md
│   ├── business
│   │   ├── 01-brd.md
│   │   ├── 02-roles-and-personas.md
│   │   └── 03-business-processes.md
│   ├── database
│   │   ├── init-script-schema-audit.md
│   │   ├── manual-patches
│   │   │   ├── 2026-07-09-order-db-sepay-schema.sql
│   │   │   └── 2026-07-09-payment-db-sepay-schema.sql
│   │   └── manual-schema-patch-order-payment-sepay.md
│   ├── decisions
│   │   └── ADR-001-product-images-cloudinary.md
│   ├── delivery
│   │   ├── 01-roadmap.md
│   │   ├── 02-mvp-acceptance.md
│   │   └── 03-migration-playbook.md
│   ├── frontend
│   │   └── 01-frontend-requirements.md
│   ├── overview
│   │   ├── 01-project-overview.md
│   │   ├── 02-glossary.md
│   │   └── 03-changelog.md
│   ├── product
│   │   ├── 01-prd.md
│   │   └── 02-user-stories.md
│   ├── README.md
│   ├── requirements
│   │   ├── 01-functional-requirements.md
│   │   ├── 02-non-functional-requirements.md
│   │   └── 03-security-requirements.md
│   ├── services
│   │   ├── ai-agent-service.md
│   │   ├── api-gateway.md
│   │   ├── auth-service.md
│   │   ├── cart-service.md
│   │   ├── notification-service.md
│   │   ├── order-service.md
│   │   ├── payment-service.md
│   │   ├── product-service.md
│   │   ├── README.md
│   │   └── user-service.md
│   └── superpowers
│       ├── plans
│       │   ├── 2026-07-06-admin-guided-product-create.md
│       │   └── 2026-07-06-product-catalog-management.md
│       └── specs
│           ├── 2026-07-06-admin-create-product-with-variants-design.md
│           ├── 2026-07-06-admin-guided-product-create-design.md
│           └── 2026-07-06-product-catalog-management-design.md
├── FE
│   ├── .env.example
│   ├── .gitignore
│   ├── index.html
│   ├── package-lock.json
│   ├── package.json
│   ├── public
│   │   ├── favicon.svg
│   │   └── icons.svg
│   ├── README.md
│   ├── src
│   │   ├── app
│   │   │   ├── App.jsx
│   │   │   ├── ProtectedRoute.jsx
│   │   │   └── router.jsx
│   │   ├── assets
│   │   │   ├── hero.png
│   │   │   └── vite.svg
│   │   ├── components
│   │   │   ├── admin
│   │   │   │   ├── AdminConfirmDialog.jsx
│   │   │   │   ├── AdminSidebar.jsx
│   │   │   │   ├── AdminTable.jsx
│   │   │   │   ├── AdminTopbar.jsx
│   │   │   │   ├── ChartCard.jsx
│   │   │   │   ├── MetricCard.jsx
│   │   │   │   └── StatusBadge.jsx
│   │   │   ├── ai
│   │   │   │   ├── AIReasoningPanel.jsx
│   │   │   │   ├── ChatBubble.jsx
│   │   │   │   ├── ProductBlock.jsx
│   │   │   │   └── PromptSuggestion.jsx
│   │   │   ├── common
│   │   │   │   ├── Badge.jsx
│   │   │   │   ├── Button.jsx
│   │   │   │   ├── Card.jsx
│   │   │   │   ├── DataTable.jsx
│   │   │   │   ├── Drawer.jsx
│   │   │   │   ├── Input.jsx
│   │   │   │   ├── Modal.jsx
│   │   │   │   └── Timeline.jsx
│   │   │   └── customer
│   │   │       ├── CartItem.jsx
│   │   │       ├── CustomerHeader.jsx
│   │   │       ├── OutfitCard.jsx
│   │   │       ├── ProductCard.jsx
│   │   │       ├── ProductFilter.jsx
│   │   │       └── ProductImage.jsx
│   │   ├── data
│   │   │   ├── mockAnalytics.js
│   │   │   ├── mockInventory.js
│   │   │   ├── mockOrders.js
│   │   │   ├── mockProducts.js
│   │   │   └── mockUsers.js
│   │   ├── features
│   │   │   ├── admin
│   │   │   │   ├── admin-error-messages.js
│   │   │   │   └── adminDashboard.api.js
│   │   │   ├── ai-stylist
│   │   │   │   ├── adminAiIndexJobs.api.js
│   │   │   │   ├── aiStylist.api.js
│   │   │   │   └── aiStylist.utils.js
│   │   │   ├── analytics
│   │   │   │   ├── analytics.api.js
│   │   │   │   └── analytics.mock.js
│   │   │   ├── auth
│   │   │   │   ├── auth.api.js
│   │   │   │   ├── auth.store.js
│   │   │   │   ├── auth.utils.js
│   │   │   │   ├── auth.validation.js
│   │   │   │   ├── auth.validation.test.js
│   │   │   │   └── passwordResetSession.js
│   │   │   ├── cart
│   │   │   │   ├── cart.api.js
│   │   │   │   ├── cart.display.js
│   │   │   │   ├── cart.display.test.js
│   │   │   │   ├── cart.mapper.js
│   │   │   │   ├── cart.mapper.test.js
│   │   │   │   ├── cart.store.js
│   │   │   │   └── cart.utils.js
│   │   │   ├── inventory
│   │   │   │   ├── inventory.api.js
│   │   │   │   └── inventory.mock.js
│   │   │   ├── notifications
│   │   │   │   └── notification.api.js
│   │   │   ├── orders
│   │   │   │   ├── admin-order.api.js
│   │   │   │   ├── order.api.js
│   │   │   │   ├── order.mock.js
│   │   │   │   ├── orderStatus.js
│   │   │   │   └── orderStatus.test.js
│   │   │   ├── payment
│   │   │   │   ├── checkoutAttempt.js
│   │   │   │   ├── checkoutAttempt.test.js
│   │   │   │   └── payment.store.js
│   │   │   ├── products
│   │   │   │   ├── admin-category.api.js
│   │   │   │   ├── admin-product-errors.js
│   │   │   │   ├── admin-product-errors.test.js
│   │   │   │   ├── admin-product-flow.js
│   │   │   │   ├── admin-product-flow.test.js
│   │   │   │   ├── admin-product-variants.js
│   │   │   │   ├── admin-product-variants.test.js
│   │   │   │   ├── admin-product.api.js
│   │   │   │   ├── product.api.js
│   │   │   │   ├── product.demographic.js
│   │   │   │   ├── product.demographic.test.js
│   │   │   │   ├── product.mapper.js
│   │   │   │   ├── product.mapper.test.js
│   │   │   │   ├── product.mock.js
│   │   │   │   ├── product.utils.js
│   │   │   │   ├── product.variant-selection.js
│   │   │   │   └── product.variant-selection.test.js
│   │   │   ├── profile
│   │   │   │   ├── profile.api.js
│   │   │   │   └── profile.mock.js
│   │   │   └── users
│   │   │       ├── user.api.js
│   │   │       └── user.store.js
│   │   ├── hooks
│   │   │   ├── useAuth.js
│   │   │   ├── useCart.js
│   │   │   └── useDebounce.js
│   │   ├── index.css
│   │   ├── layouts
│   │   │   ├── AdminLayout.jsx
│   │   │   ├── AuthLayout.jsx
│   │   │   └── CustomerLayout.jsx
│   │   ├── main.jsx
│   │   ├── pages
│   │   │   ├── admin
│   │   │   │   ├── AdminDashboardPage.jsx
│   │   │   │   ├── AdminSettingsPage.jsx
│   │   │   │   ├── AIPipelinePage.jsx
│   │   │   │   ├── KnowledgeGraphPage.jsx
│   │   │   │   ├── NotificationManagementPage.jsx
│   │   │   │   ├── OrderManagementPage.jsx
│   │   │   │   ├── ProductManagementPage.jsx
│   │   │   │   ├── RecommendationAnalyticsPage.jsx
│   │   │   │   └── UserManagementPage.jsx
│   │   │   ├── auth
│   │   │   │   ├── ForgotPasswordPage.jsx
│   │   │   │   ├── LoginPage.jsx
│   │   │   │   ├── PasswordRecoveryShell.jsx
│   │   │   │   ├── RegisterPage.jsx
│   │   │   │   ├── resetPassword.utils.js
│   │   │   │   ├── resetPassword.utils.test.js
│   │   │   │   ├── ResetPasswordPage.jsx
│   │   │   │   ├── StyleProfilePage.jsx
│   │   │   │   └── VerifyResetOtpPage.jsx
│   │   │   └── customer
│   │   │       ├── AIStylistChatPage.jsx
│   │   │       ├── CartPage.jsx
│   │   │       ├── CheckoutPage.jsx
│   │   │       ├── HomePage.jsx
│   │   │       ├── NotificationsPage.jsx
│   │   │       ├── OrderTrackingPage.jsx
│   │   │       ├── ProductCatalogPage.jsx
│   │   │       └── ProductDetailPage.jsx
│   │   ├── services
│   │   │   ├── apiClient.js
│   │   │   └── endpoints.js
│   │   └── utils
│   │       ├── constants.js
│   │       ├── formatCurrency.js
│   │       ├── formatCurrency.test.js
│   │       └── formatDate.js
│   └── vite.config.js
├── full-up.bat
├── FULLSYSTEM_START_GUIDE.md
├── generate-rsa-keys.ps1
├── PROJECT_SNAPSHOT_FOR_REVIEW.md
├── README-WINDOWS.md
├── README.md
├── scripts
│   ├── full-up.sh
│   └── windows
│       ├── full-down.ps1
│       ├── full-up.ps1
│       └── logs.ps1
└── skills-lock.json
```

---

## 2. CẤU HÌNH DOCKER & COMPOSE

### Dockerfile Files

#### BE\ai-agent-service\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY ai-agent-service/src ai-agent-service/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl ai-agent-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/ai-agent-service/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]

```

#### BE\ai-agent-service\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY ai-agent-service/src ai-agent-service/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl ai-agent-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/ai-agent-service/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]

```

#### BE\api-gateway\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY api-gateway/src api-gateway/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl api-gateway -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/api-gateway/target/api-gateway-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 3001
ENTRYPOINT ["java", "-jar", "app.jar"]

```

#### BE\api-gateway\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY api-gateway/src api-gateway/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl api-gateway -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/api-gateway/target/api-gateway-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 3001
ENTRYPOINT ["java", "-jar", "app.jar"]

```

#### BE\auth-service\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY auth-service/src auth-service/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl auth-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/auth-service/target/auth-service-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

```

#### BE\auth-service\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY auth-service/src auth-service/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl auth-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/auth-service/target/auth-service-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

```

#### BE\cart-service\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY cart-service/src cart-service/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl cart-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/cart-service/target/cart-service-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### BE\cart-service\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY cart-service/src cart-service/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl cart-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/cart-service/target/cart-service-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### BE\notification-service\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY notification-service/src notification-service/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl notification-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/notification-service/target/notification-service-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]

```

#### BE\notification-service\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY notification-service/src notification-service/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl notification-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/notification-service/target/notification-service-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]

```

#### BE\order-service\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY cart-service/src cart-service/src
COPY order-service/src order-service/src
RUN mvn clean install -pl common-lib,cart-service -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl order-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/order-service/target/order-service-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]

```

#### BE\order-service\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY cart-service/src cart-service/src
COPY order-service/src order-service/src
RUN mvn clean install -pl common-lib,cart-service -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl order-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/order-service/target/order-service-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]

```

#### BE\payment-service\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY payment-service/src payment-service/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl payment-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/payment-service/target/payment-service-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/payment_db
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=password
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]

```

#### BE\payment-service\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY payment-service/src payment-service/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl payment-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/payment-service/target/payment-service-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/payment_db
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=password
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]

```

#### BE\product-service\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY product-service/src product-service/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl product-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/product-service/target/product-service-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/product_db
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=password
ENV S3_ENDPOINT=http://host.docker.internal:9000
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]

```

#### BE\product-service\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY product-service/src product-service/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl product-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/product-service/target/product-service-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/product_db
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=password
ENV S3_ENDPOINT=http://host.docker.internal:9000
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]

```

#### BE\user-service\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY user-service/src user-service/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl user-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/user-service/target/user-service-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]

```

#### BE\user-service\Dockerfile

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ai-agent-service/pom.xml ai-agent-service/pom.xml
RUN mvn dependency:go-offline -B

COPY common-lib/src common-lib/src
COPY user-service/src user-service/src
RUN mvn clean install -pl common-lib -am -DskipTests -Dspring-boot.repackage.skip=true && mvn clean package spring-boot:repackage -pl user-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=builder /app/user-service/target/user-service-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]

```

### Docker Compose Files

#### BE\docker-compose.infra.yml

```yaml
services:
  # ----------------------------------------------------
  # 1. Infrastructure: Databases & Message Broker
  # ----------------------------------------------------
  postgres:
    image: postgres:15-alpine
    container_name: stylemind-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: stylemind-redis
    ports:
      - "6379:6379"
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  qdrant:
    image: qdrant/qdrant:latest
    container_name: stylemind-qdrant
    ports:
      - "6333:6333"
      - "6334:6334"
    volumes:
      - qdrant_data:/qdrant/storage
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD-SHELL", "bash -ec ': >/dev/tcp/localhost/6333'"]
      interval: 10s
      timeout: 5s
      retries: 5

  neo4j:
    image: neo4j:5-community
    container_name: stylemind-neo4j
    ports:
      - "7474:7474"
      - "7687:7687"
    environment:
      NEO4J_AUTH: neo4j/password
      NEO4J_PLUGINS: '["apoc"]'
      NEO4J_dbms_memory_heap_max__size: 1G
    volumes:
      - neo4j_data:/data
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD", "cypher-shell", "-u", "neo4j", "-p", "password", "RETURN 1"]
      interval: 15s
      timeout: 10s
      retries: 10

  minio:
    image: minio/minio:latest
    container_name: stylemind-minio
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: admin
      MINIO_ROOT_PASSWORD: password
    command: server /data --console-address ":9001"
    volumes:
      - minio_data:/data
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 10s
      timeout: 5s
      retries: 5

networks:
  stylemind-network:
    driver: bridge

volumes:
  pgdata:
  qdrant_data:
  neo4j_data:
  minio_data:

```

#### BE\docker-compose.yml

```yaml
services:
  # ----------------------------------------------------
  # 1. Infrastructure Services (Profile: infra, all)
  # ----------------------------------------------------
  redis:
    image: redis:7-alpine
    container_name: stylemind-redis
    ports:
      - "6379:6379"
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    profiles:
      - infra
      - app
      - all

  qdrant:
    image: qdrant/qdrant:latest
    container_name: stylemind-qdrant
    ports:
      - "6333:6333"
      - "6334:6334"
    volumes:
      - qdrant_data:/qdrant/storage
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:6333/ || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 5
    profiles:
      - infra
      - app
      - all

  neo4j:
    image: neo4j:5-community
    container_name: stylemind-neo4j
    ports:
      - "7474:7474"
      - "7687:7687"
    environment:
      NEO4J_AUTH: neo4j/password
      NEO4J_PLUGINS: '["apoc"]'
      NEO4J_dbms_memory_heap_max__size: 1G
    volumes:
      - neo4j_data:/data
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD", "cypher-shell", "-u", "neo4j", "-p", "password", "RETURN 1"]
      interval: 15s
      timeout: 10s
      retries: 10
    profiles:
      - infra
      - app
      - all

  minio:
    image: minio/minio:latest
    container_name: stylemind-minio
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: admin
      MINIO_ROOT_PASSWORD: password
    command: server /data --console-address ":9001"
    volumes:
      - minio_data:/data
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 10s
      timeout: 5s
      retries: 5
    profiles:
      - infra
      - app
      - all

  # ----------------------------------------------------
  # 2. PostgreSQL Databases (Profile: infra, all)
  # ----------------------------------------------------
  postgres-auth:
    image: postgres:15-alpine
    container_name: postgres-auth
    ports:
      - "5433:5432"
    environment:
      POSTGRES_DB: auth_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata-auth:/var/lib/postgresql/data
      - ./init-scripts/01-auth-db.sql:/docker-entrypoint-initdb.d/01-auth-db.sql
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d auth_db"]
      interval: 10s
      timeout: 5s
      retries: 5
    profiles:
      - infra
      - app
      - all

  postgres-user:
    image: postgres:15-alpine
    container_name: postgres-user
    ports:
      - "5434:5432"
    environment:
      POSTGRES_DB: user_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata-user:/var/lib/postgresql/data
      - ./init-scripts/02-user-db.sql:/docker-entrypoint-initdb.d/02-user-db.sql
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d user_db"]
      interval: 10s
      timeout: 5s
      retries: 5
    profiles:
      - infra
      - app
      - all

  postgres-product:
    image: postgres:15-alpine
    container_name: postgres-product
    ports:
      - "5435:5432"
    environment:
      POSTGRES_DB: product_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata-product:/var/lib/postgresql/data
      - ./init-scripts/03-product-db.sql:/docker-entrypoint-initdb.d/03-product-db.sql
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d product_db"]
      interval: 10s
      timeout: 5s
      retries: 5
    profiles:
      - infra
      - app
      - all

  postgres-cart:
    image: postgres:15-alpine
    container_name: postgres-cart
    ports:
      - "5436:5432"
    environment:
      POSTGRES_DB: cart_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata-cart:/var/lib/postgresql/data
      - ./init-scripts/05-cart-db.sql:/docker-entrypoint-initdb.d/05-cart-db.sql
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d cart_db"]
      interval: 10s
      timeout: 5s
      retries: 5
    profiles:
      - infra
      - app
      - all

  postgres-order:
    image: postgres:15-alpine
    container_name: postgres-order
    ports:
      - "5437:5432"
    environment:
      POSTGRES_DB: order_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata-order:/var/lib/postgresql/data
      - ./init-scripts/06-order-db.sql:/docker-entrypoint-initdb.d/06-order-db.sql
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d order_db"]
      interval: 10s
      timeout: 5s
      retries: 5
    profiles:
      - infra
      - app
      - all

  postgres-payment:
    image: postgres:15-alpine
    container_name: postgres-payment
    ports:
      - "5438:5432"
    environment:
      POSTGRES_DB: payment_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata-payment:/var/lib/postgresql/data
      - ./init-scripts/07-payment-db.sql:/docker-entrypoint-initdb.d/07-payment-db.sql
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d payment_db"]
      interval: 10s
      timeout: 5s
      retries: 5
    profiles:
      - infra
      - app
      - all

  postgres-ai:
    image: postgres:15-alpine
    container_name: postgres-ai
    ports:
      - "5439:5432"
    environment:
      POSTGRES_DB: ai_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata-ai:/var/lib/postgresql/data
      - ./init-scripts/08-ai-db.sql:/docker-entrypoint-initdb.d/08-ai-db.sql
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d ai_db"]
      interval: 10s
      timeout: 5s
      retries: 5
    profiles:
      - infra
      - app
      - all

  postgres-notification:
    image: postgres:15-alpine
    container_name: postgres-notification
    ports:
      - "5440:5432"
    environment:
      POSTGRES_DB: notification_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata-notification:/var/lib/postgresql/data
      - ./init-scripts/09-notification-db.sql:/docker-entrypoint-initdb.d/09-notification-db.sql
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d notification_db"]
      interval: 10s
      timeout: 5s
      retries: 5
    profiles:
      - infra
      - app
      - all

  # ----------------------------------------------------
  # 4. API Gateway (Profile: app, all)
  # ----------------------------------------------------
  api-gateway:
    build:
      context: .
      dockerfile: api-gateway/Dockerfile
    image: stylemind/api-gateway:latest
    container_name: stylemind-gateway
    ports:
      - "3000:3000"
      - "5005:5005"
    environment:
      SERVER_PORT: 3000
      JWT_PUBLIC_KEY_PATH: /app/certs/public_key.pem
      JWT_ALGORITHM: RSA
      JWT_KEY_SIZE: 2048
      JWT_ACCESS_TOKEN_EXPIRATION: 3600000
      JWT_REFRESH_TOKEN_EXPIRATION: 604800000
      AUTH_SERVICE_URL: http://auth-service:8081
      PRODUCT_SERVICE_URL: http://product-service:8083
      USER_SERVICE_URL: http://user-service:8082
      CART_SERVICE_URL: http://cart-service:8086
      ORDER_SERVICE_URL: http://order-service:8087
      AI_SERVICE_URL: http://ai-agent-service:8085
      PAYMENT_SERVICE_URL: http://payment-service:8088
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://discovery-service:8761/eureka
      REDIS_HOST: stylemind-redis
      REDIS_PORT: 6379
    volumes:
      - ./.docker/certs:/app/certs:ro
    networks:
      - stylemind-network
    profiles:
      - app
      - all

  # ----------------------------------------------------
  # 5. Auth Service (Profile: app, all)
  # ----------------------------------------------------
  auth-service:
    build:
      context: .
      dockerfile: auth-service/Dockerfile
    image: stylemind/auth-service:latest
    container_name: auth-service
    ports:
      - "8081:8081"
      - "5006:5005"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-auth:5432/auth_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
      JWT_PRIVATE_KEY_PATH: /app/certs/private_key.pem
      JWT_PUBLIC_KEY_PATH: /app/certs/public_key.pem
      JWT_ALGORITHM: RSA
      JWT_KEY_SIZE: 2048
      JWT_ACCESS_TOKEN_EXPIRATION: 3600000
      JWT_REFRESH_TOKEN_EXPIRATION: 604800000
      SERVER_PORT: 8081
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://discovery-service:8761/eureka
      SPRING_MAIN_ALLOW_CIRCULAR_REFERENCES: "true"
    volumes:
      - ./.docker/certs:/app/certs:ro
    depends_on:
      postgres-auth:
        condition: service_healthy
    networks:
      - stylemind-network
    profiles:
      - app
      - all

  # ----------------------------------------------------
  # 6. Business Services (Profile: app, all)
  # ----------------------------------------------------
  user-service:
    build:
      context: .
      dockerfile: user-service/Dockerfile
    image: stylemind/user-service:latest
    container_name: user-service
    ports:
      - "8082:8082"
      - "5007:5005"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-user:5432/user_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
      JWT_PUBLIC_KEY_PATH: /app/certs/public_key.pem
      JWT_ALGORITHM: RSA
      JWT_KEY_SIZE: 2048
      JWT_ACCESS_TOKEN_EXPIRATION: 3600000
      JWT_REFRESH_TOKEN_EXPIRATION: 604800000
      SERVER_PORT: 8082
      AUTH_SERVICE_URL: http://auth-service:8081
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://discovery-service:8761/eureka
    volumes:
      - ./.docker/certs:/app/certs:ro
    depends_on:
      postgres-user:
        condition: service_healthy
    networks:
      - stylemind-network
    profiles:
      - app
      - all

  product-service:
    build:
      context: .
      dockerfile: product-service/Dockerfile
    image: stylemind/product-service:latest
    container_name: product-service
    ports:
      - "8083:8083"
      - "5008:5005"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-product:5432/product_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
      JWT_PUBLIC_KEY_PATH: /app/certs/public_key.pem
      JWT_ALGORITHM: RSA
      JWT_KEY_SIZE: 2048
      JWT_ACCESS_TOKEN_EXPIRATION: 3600000
      JWT_REFRESH_TOKEN_EXPIRATION: 604800000
      S3_ENDPOINT: http://minio:9000
      S3_ACCESS_KEY: admin
      S3_SECRET_KEY: password
      S3_BUCKET: stylemind-products
      INTERNAL_TOKEN: sm-secret-internal-service-token-key-2026
      SERVER_PORT: 8083
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://discovery-service:8761/eureka
    volumes:
      - ./.docker/certs:/app/certs:ro
    depends_on:
      postgres-product:
        condition: service_healthy
      minio:
        condition: service_healthy
    networks:
      - stylemind-network
    profiles:
      - app
      - all

  cart-service:
    build:
      context: .
      dockerfile: cart-service/Dockerfile
    image: stylemind/cart-service:latest
    container_name: cart-service
    ports:
      - "8086:8086"
      - "5011:5005"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-cart:5432/cart_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
      JWT_PUBLIC_KEY_PATH: /app/certs/public_key.pem
      JWT_ALGORITHM: RSA
      JWT_KEY_SIZE: 2048
      JWT_ACCESS_TOKEN_EXPIRATION: 3600000
      JWT_REFRESH_TOKEN_EXPIRATION: 604800000
      SERVER_PORT: 8086
      AUTH_SERVICE_URL: http://auth-service:8081
      PRODUCT_SERVICE_URL: http://product-service:8083
      INTERNAL_TOKEN: sm-secret-internal-service-token-key-2026
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://discovery-service:8761/eureka
    volumes:
      - ./.docker/certs:/app/certs:ro
    depends_on:
      postgres-cart:
        condition: service_healthy
    networks:
      - stylemind-network
    profiles:
      - app
      - all

  order-service:
    build:
      context: .
      dockerfile: order-service/Dockerfile
    image: stylemind/order-service:latest
    container_name: order-service
    ports:
      - "8087:8087"
      - "5012:5005"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-order:5432/order_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
      JWT_PUBLIC_KEY_PATH: /app/certs/public_key.pem
      JWT_ALGORITHM: RSA
      JWT_KEY_SIZE: 2048
      JWT_ACCESS_TOKEN_EXPIRATION: 3600000
      JWT_REFRESH_TOKEN_EXPIRATION: 604800000
      PAYMENT_SERVICE_URL: http://payment-service:8088
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://discovery-service:8761/eureka
      CART_SERVICE_URL: http://cart-service:8086
      INTERNAL_TOKEN: sm-secret-internal-service-token-key-2026
      SERVER_PORT: 8087
    volumes:
      - ./.docker/certs:/app/certs:ro
    depends_on:
      postgres-order:
        condition: service_healthy
    networks:
      - stylemind-network
    profiles:
      - app
      - all

  payment-service:
    build:
      context: .
      dockerfile: payment-service/Dockerfile
    image: stylemind/payment-service:latest
    container_name: payment-service
    ports:
      - "8088:8088"
      - "5013:5005"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-payment:5432/payment_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
      JWT_PUBLIC_KEY_PATH: /app/certs/public_key.pem
      JWT_ALGORITHM: RSA
      JWT_KEY_SIZE: 2048
      JWT_ACCESS_TOKEN_EXPIRATION: 3600000
      JWT_REFRESH_TOKEN_EXPIRATION: 604800000
      INTERNAL_TOKEN: sm-secret-internal-service-token-key-2026
      SERVER_PORT: 8088
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://discovery-service:8761/eureka
      SEPAY_BANK_SHORT_NAME: VCB
      SEPAY_ACCOUNT_NUMBER: 1234567890
      SEPAY_ACCOUNT_NAME: TEST_ACCOUNT
      SEPAY_BANK_HUB_PREFIX: SEVQR
      SEPAY_PAYMENT_EXPIRE_MINUTES: 15
      SEPAY_QR_BASE_URL: https://img.vietqr.io/image
      SEPAY_ENABLED: false
      SEPAY_MODE: test
      SEPAY_PAYMENT_CODE_PREFIX: STYLEMIND
      SEPAY_WEBHOOK_AUTH_MODE: API_KEY
      SEPAY_WEBHOOK_API_KEY: test-key
    volumes:
      - ./.docker/certs:/app/certs:ro
    depends_on:
      postgres-payment:
        condition: service_healthy
    networks:
      - stylemind-network
    profiles:
      - app
      - all

  notification-service:
    build:
      context: .
      dockerfile: notification-service/Dockerfile
    image: stylemind/notification-service:latest
    container_name: notification-service
    ports:
      - "8089:8089"
      - "5014:5005"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-notification:5432/notification_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
      JWT_PUBLIC_KEY_PATH: /app/certs/public_key.pem
      JWT_ALGORITHM: RSA
      JWT_KEY_SIZE: 2048
      JWT_ACCESS_TOKEN_EXPIRATION: 3600000
      JWT_REFRESH_TOKEN_EXPIRATION: 604800000
      SERVER_PORT: 8089
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://discovery-service:8761/eureka
      MAIL_ENABLED: false
      SPRING_AUTOCONFIGURE_EXCLUDE: org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration
      MANAGEMENT_HEALTH_MAIL_ENABLED: false
    volumes:
      - ./.docker/certs:/app/certs:ro
    depends_on:
      postgres-notification:
        condition: service_healthy
    networks:
      - stylemind-network
    profiles:
      - app
      - all

  # ----------------------------------------------------
  # 7. AI Agent Service (Profile: app, all)
  # ----------------------------------------------------
  ai-agent-service:
    build:
      context: .
      dockerfile: ai-agent-service/Dockerfile
    image: stylemind/ai-agent-service:latest
    container_name: ai-agent-service
    ports:
      - "8085:8085"
      - "5010:5005"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-ai:5432/ai_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
      JWT_PUBLIC_KEY_PATH: /app/certs/public_key.pem
      JWT_ALGORITHM: RSA
      JWT_KEY_SIZE: 2048
      JWT_ACCESS_TOKEN_EXPIRATION: 3600000
      JWT_REFRESH_TOKEN_EXPIRATION: 604800000
      QDRANT_HOST: qdrant
      QDRANT_PORT: 6333
      NEO4J_URI: bolt://neo4j:7687
      NEO4J_USERNAME: neo4j
      NEO4J_PASSWORD: password
      LLM_API_KEY: your-llm-api-key-here
      INTERNAL_TOKEN: sm-secret-internal-service-token-key-2026
      PRODUCT_SERVICE_URL: http://product-service:8083
      ORDER_SERVICE_URL: http://order-service:8087
      SERVER_PORT: 8085
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://discovery-service:8761/eureka
    volumes:
      - ./.docker/certs:/app/certs:ro
    depends_on:
      postgres-ai:
        condition: service_healthy
      neo4j:
        condition: service_healthy
    networks:
      - stylemind-network
    profiles:
      - app
      - all

networks:
  stylemind-network:
    driver: bridge

volumes:
  pgdata-auth:
  pgdata-user:
  pgdata-product:
  pgdata-cart:
  pgdata-order:
  pgdata-payment:
  pgdata-ai:
  pgdata-notification:
  qdrant_data:
  neo4j_data:
  minio_data:

```

---

## 3. CÁC SCRIPT VÀ HARNESS FILES

### Script Files

#### BE\scripts\migrations\migrate-auth-full-name-to-user-profile.sh

```bash
#!/usr/bin/env bash
set -euo pipefail

AUTH_DATABASE_URL="${AUTH_DATABASE_URL:-postgresql://postgres:password@localhost:5432/auth_db}"
USER_DATABASE_URL="${USER_DATABASE_URL:-postgresql://postgres:password@localhost:5432/user_db}"
transfer_file="$(mktemp)"
trap 'rm -f "$transfer_file"' EXIT

psql "$USER_DATABASE_URL" -v ON_ERROR_STOP=1 -c \
  "ALTER TABLE customer_style_profiles ADD COLUMN IF NOT EXISTS display_name VARCHAR(150)"

psql "$AUTH_DATABASE_URL" -v ON_ERROR_STOP=1 \
  -c "\\copy (SELECT id, full_name FROM users WHERE full_name IS NOT NULL) TO '$transfer_file' WITH (FORMAT csv)"

psql "$USER_DATABASE_URL" -v ON_ERROR_STOP=1 <<SQL
CREATE TEMP TABLE profile_name_transfer (
    user_id VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(150)
);
\copy profile_name_transfer FROM '$transfer_file' WITH (FORMAT csv)
INSERT INTO customer_style_profiles (user_id, display_name, created_at, updated_at)
SELECT user_id, display_name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM profile_name_transfer
ON CONFLICT (user_id) DO UPDATE
SET display_name = COALESCE(customer_style_profiles.display_name, EXCLUDED.display_name),
    updated_at = CURRENT_TIMESTAMP;
SQL

psql "$AUTH_DATABASE_URL" -v ON_ERROR_STOP=1 -c \
  "UPDATE users SET full_name = NULL WHERE full_name IS NOT NULL"

echo "Transferred auth full names into user profile shells."

```

#### BE\scripts\verify-adoption-docs.py

```python
#!/usr/bin/env python3
"""Validate adoption docs contain adoption and agent instruction contracts."""

from __future__ import annotations

from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    full_path = ROOT / path
    if not full_path.exists():
        raise AssertionError(f"missing required file: {path}")
    return full_path.read_text(encoding="utf-8")


def require(text: str, needle: str, path: str) -> None:
    if needle not in text:
        raise AssertionError(f"{path}: missing required text: {needle}")


def main() -> int:
    walkthrough_path = "docs/adoption/clean-clone-walkthrough.md"
    walkthrough = read(walkthrough_path)
    example_path = "docs/examples/full-agent-workflow.md"
    example = read(example_path)
    agent_paths = [
        "docs/agents/codex.md",
        "docs/agents/claude-code.md",
        "docs/agents/cursor.md",
    ]
    agents = {path: read(path) for path in agent_paths}
    agents_text = "\n".join(agents.values())
    troubleshooting_path = "docs/troubleshooting.md"
    troubleshooting = read(troubleshooting_path)
    cookbook_path = "docs/COMMAND_COOKBOOK.md"
    cookbook = read(cookbook_path)
    agents_index = read("AGENTS.md")
    readme = read("README.md")
    docs_readme = read("docs/README.md")
    scripts_readme = read("scripts/README.md")
    archive_path = ROOT / "docs/archive/README.md"
    archive_readme = (
        archive_path.read_text(encoding="utf-8") if archive_path.exists() else None
    )
    source_has_packaging_tooling = (
        ROOT / "scripts/build-production-payload.py"
    ).exists()

    readme_needles = [
        "# Harness Intelligence OS",
        "Sovereign Identity",
        "hios.toml",
        "harness-cli identity",
        "5-Minute Quickstart",
        "intake",
        "context",
        "story verify",
        "trace",
        "governance dashboard",
        "docs/adoption/clean-clone-walkthrough.md",
        "docs/examples/full-agent-workflow.md",
        "docs/agents/codex.md",
        "docs/agents/claude-code.md",
        "docs/agents/cursor.md",
        "docs/troubleshooting.md",
        "docs/COMMAND_COOKBOOK.md",
        "docs/archive/",
        "release verify --version 0.7.0",
        "Governance Dashboard",
        "CodeGraph",
        "NotebookLM",
        "inconclusive",
        "Google credentials",
        "ntu254/Harness-Intelligence-OS",
        "v0.7: Adoption Ready",
        "build-production-payload.sh --version 0.7.0",
        "verify-production-payload.py --version 0.7.0 --source-check",
        "packaging/production-include.toml",
    ]

    for needle in readme_needles:
        require(readme, needle, "README.md")

    walkthrough_needles = [
        "git clone https://github.com/ntu254/Harness-Intelligence-OS.git",
        "cargo build --package harness-cli --release",
        "harness-cli.exe init",
        "import brownfield",
        "query matrix",
        "US-DEMO",
        "context --story US-DEMO",
        "arch-check --story US-DEMO",
        "trace",
        "story verify US-DEMO",
        "governance report",
        "governance dashboard",
        "release verify --version 0.7.0",
        "CodeGraph",
        "NotebookLM",
        "inconclusive",
        "harness.db",
        ".harness/",
        "Do not commit",
        "Google credentials",
        "Production Payload Path",
        "hios-production-v0.7.0.zip",
        "spec.md",
    ]

    for needle in walkthrough_needles:
        require(walkthrough, needle, walkthrough_path)

    example_needles = [
        "# Full Agent Workflow Example",
        "US-EXAMPLE",
        "intake",
        "story add",
        "Optional Provider Context",
        "codegraph impact",
        "notebooklm brief",
        "context --story US-EXAMPLE",
        "cargo test --workspace",
        "story update",
        "trace",
        "story verify US-EXAMPLE",
        "governance report",
        "governance dashboard",
        "Expected output",
        "Provider Troubleshooting",
        "CodeGraph Unavailable",
        "NotebookLM Auth Or Session Missing",
        "Context ingest: inconclusive",
        "Context ingest: fail",
        "Google credentials",
        "provider session files",
    ]

    for needle in example_needles:
        require(example, needle, example_path)

    for path, text in agents.items():
        agent_needles = [
            "Startup Checklist",
            "context --story US-XXX",
            "Do not code before",
            "story verify",
            "inconclusive",
            "pass",
            "Google credentials",
            "provider session files",
            "Verification Discipline",
        ]
        for needle in agent_needles:
            require(text, needle, path)

    for path in agent_paths:
        require(readme, path, "README.md")
        require(agents_index, path, "AGENTS.md")

    troubleshooting_needles = [
        "# Troubleshooting",
        "Installer Fails",
        "Release Verify Fails",
        "CodeGraph Unavailable",
        "NotebookLM Auth Or Session Fails",
        "NotebookLM Output Fails Validation",
        "Governance Gate Fails",
        "Governance Report Or Dashboard Fails",
        "checksum mismatch",
        "release verify --version 0.7.0",
        "Context ingest: inconclusive",
        "Context ingest: fail",
        "story verify US-XXX",
        "verify-governance-report-schema.py",
        "harness.db",
        ".harness/",
        "Google credentials",
        "provider session files",
        "Do not weaken the story gate",
    ]

    for needle in troubleshooting_needles:
        require(troubleshooting, needle, troubleshooting_path)

    cookbook_needles = [
        "# Command Cookbook",
        "harness-cli identity",
        "## Intake",
        "## Context",
        "## Verify",
        "## Trace",
        "## Release",
        "## Dashboard",
        "## MCP / Provider Evidence",
        "harness-cli intake",
        "harness-cli context --story",
        "harness-cli story verify",
        "harness-cli trace",
        "release verify --version 0.7.0",
        "governance report",
        "governance dashboard",
        "codegraph impact",
        "notebooklm brief",
        "inconclusive",
        "Google credentials",
        "provider session files",
        ".\\scripts\\bin\\harness-cli.exe",
        "build-production-payload.sh --version 0.7.0",
        "verify-production-payload.py --version 0.7.0 --source-check",
        "hios-production-v0.7.0.zip",
    ]

    for needle in cookbook_needles:
        require(cookbook, needle, cookbook_path)

    require(
        readme,
        "docs/adoption/clean-clone-walkthrough.md",
        "README.md",
    )
    require(readme, "docs/examples/full-agent-workflow.md", "README.md")
    require(readme, "docs/troubleshooting.md", "README.md")
    require(readme, "docs/COMMAND_COOKBOOK.md", "README.md")
    require(docs_readme, "adoption/", "docs/README.md")
    require(docs_readme, "examples/", "docs/README.md")
    require(docs_readme, "agents/", "docs/README.md")
    require(docs_readme, "troubleshooting.md", "docs/README.md")
    require(docs_readme, "COMMAND_COOKBOOK.md", "docs/README.md")
    require(docs_readme, "hios.toml", "docs/README.md")
    require(docs_readme, "archive/", "docs/README.md")
    if archive_readme is not None:
        require(
            archive_readme,
            "historical planning documents",
            "docs/archive/README.md",
        )
        require(
            archive_readme,
            "not the current operating entrypoint",
            "docs/archive/README.md",
        )
    require(scripts_readme, "harness-cli identity", "scripts/README.md")
    require(scripts_readme, "hios.toml", "scripts/README.md")
    require(scripts_readme, "Production-Clean Payload", "scripts/README.md")
    require(
        scripts_readme,
        "packaging/production-include.toml",
        "scripts/README.md",
    )
    if source_has_packaging_tooling:
        production_contract = read("packaging/production-include.toml")
        production_builder = read("scripts/build-production-payload.py")
        production_verifier = read("scripts/verify-production-payload.py")
        require(
            production_contract,
            'name = "hios-production"',
            "packaging/production-include.toml",
        )
        require(
            production_contract,
            '"spec.md"',
            "packaging/production-include.toml",
        )
        require(
            production_builder,
            "write_archive",
            "scripts/build-production-payload.py",
        )
        require(
            production_verifier,
            "source-check",
            "scripts/verify-production-payload.py",
        )
    require(agents_text, "Codex", "docs/agents/*")
    require(agents_text, "Claude Code", "docs/agents/*")
    require(agents_text, "Cursor", "docs/agents/*")
    require(
        scripts_readme,
        "python scripts/verify-adoption-docs.py",
        "scripts/README.md",
    )

    print("Adoption docs verification passed.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"Adoption docs verification failed: {exc}", file=sys.stderr)
        raise SystemExit(1)

```

#### BE\scripts\verify-api-versioning.py

```python
#!/usr/bin/env python3

from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[2]
IGNORED_PARTS = {".git", ".codegraph", "node_modules", "target", "dist"}

UNVERSIONED_PATH = re.compile(r'["\']/(api|internal)/(?!v1(?:/|["\']))')
UNVERSIONED_GATEWAY_PATH = re.compile(r"Path=/(api|internal)/(?!v1(?:/|\*|$))")
FRONTEND_SERVICE_PORT = re.compile(r"http://localhost:808\d")


def source_files():
    roots = (
        ROOT / "BE",
        ROOT / "FE" / "src",
        ROOT / "FE" / ".env.example",
    )
    for root in roots:
        candidates = [root] if root.is_file() else root.rglob("*")
        for path in candidates:
            if not path.is_file() or any(part in IGNORED_PARTS for part in path.parts):
                continue
            if path.suffix in {".java", ".yml", ".yaml", ".js", ".jsx"} or path.name.startswith(".env"):
                yield path


def main():
    failures = []

    for path in source_files():
        text = path.read_text(encoding="utf-8")
        relative_path = path.relative_to(ROOT)
        for line_number, line in enumerate(text.splitlines(), start=1):
            if UNVERSIONED_PATH.search(line) or UNVERSIONED_GATEWAY_PATH.search(line):
                failures.append(f"{relative_path}:{line_number}: unversioned API path: {line.strip()}")

            if relative_path.parts[0] == "FE" and FRONTEND_SERVICE_PORT.search(line):
                failures.append(f"{relative_path}:{line_number}: frontend service port: {line.strip()}")

    api_client = (ROOT / "FE/src/services/apiClient.js").read_text(encoding="utf-8")
    endpoints = (ROOT / "FE/src/services/endpoints.js").read_text(encoding="utf-8")
    env_example = (ROOT / "FE/.env.example").read_text(encoding="utf-8")

    if "baseURL:" not in api_client or "VITE_API_BASE_URL" not in api_client:
        failures.append("FE/src/services/apiClient.js: Axios must own VITE_API_BASE_URL as baseURL")
    if "VITE_API_GATEWAY" in endpoints or "http://localhost" in endpoints:
        failures.append("FE/src/services/endpoints.js: endpoint paths must not construct gateway origins")
    if "/api/v1/" not in endpoints:
        failures.append("FE/src/services/endpoints.js: endpoint paths must use /api/v1")
    if "VITE_API_GATEWAY" in env_example:
        failures.append("FE/.env.example: VITE_API_GATEWAY must be removed")

    if failures:
        print("\n".join(failures))
        return 1

    print("API versioning contract verified")
    return 0


if __name__ == "__main__":
    sys.exit(main())

```

#### BE\scripts\verify-friction-taxonomy.py

```python
#!/usr/bin/env python3
import copy
import json
from pathlib import Path

from jsonschema import Draft202012Validator, FormatChecker


ROOT = Path(__file__).resolve().parents[1]
SCHEMA = ROOT / "docs" / "schemas" / "friction-event.schema.json"
TAXONOMY_DOC = ROOT / "docs" / "FRICTION_TAXONOMY.md"
DECISION_DOC = ROOT / "docs" / "decisions" / "0011-harness-friction-taxonomy.md"
EVENT_TYPES = [
    "missing_context",
    "ambiguous_policy",
    "weak_validation",
    "provider_unavailable",
    "schema_gap",
    "release_gap",
    "architecture_rule_gap",
    "repeated_manual_step",
]


def expect_valid(validator, instance, label):
    errors = sorted(validator.iter_errors(instance), key=lambda error: list(error.path))
    if errors:
        raise AssertionError(f"{label} should be valid: {errors[0].message}")


def expect_invalid(validator, instance, label):
    if not list(validator.iter_errors(instance)):
        raise AssertionError(f"{label} should be invalid")


def expect_event_types_documented(schema):
    schema_types = schema["properties"]["friction_type"]["enum"]
    if schema_types != EVENT_TYPES:
        raise AssertionError("schema friction_type enum does not match canonical order")

    taxonomy = TAXONOMY_DOC.read_text(encoding="utf-8")
    decision = DECISION_DOC.read_text(encoding="utf-8")
    for event_type in EVENT_TYPES:
        if f"`{event_type}`" not in taxonomy:
            raise AssertionError(f"{event_type} missing from taxonomy doc")
        if f"`{event_type}`" not in decision:
            raise AssertionError(f"{event_type} missing from Decision 0011")


event = {
    "schema_version": "1.0.0",
    "artifact_type": "friction-event",
    "event_id": "44444444-4444-4444-8444-444444444444",
    "story_id": "US-029",
    "trace_id": 25,
    "friction_type": "provider_unavailable",
    "severity": "high",
    "source": "trace",
    "summary": "NotebookLM default profile was unavailable during release hardening.",
    "observed_at": "2026-06-07T00:00:00Z",
    "provider": "notebooklm-mcp-cli",
    "affected_paths": [
        "docs/stories/US-026/validation.md",
        "docs/stories/US-028/validation.md",
    ],
    "evidence": {
        "command": "nlm login --check",
        "exit_code": 1,
        "artifact_path": ".harness/context/US-028-notebooklm-brief.json",
        "report_path": ".harness/context/US-028-notebooklm-ingest-result.json",
        "trace_id": 25,
        "details": "Profile not found: default",
    },
    "proposed_action": {
        "action_type": "provider_preflight",
        "title": "Add NotebookLM provider preflight",
        "target_path": "docs/stories/US-030/overview.md",
    },
}


with SCHEMA.open(encoding="utf-8") as handle:
    loaded_schema = json.load(handle)
Draft202012Validator.check_schema(loaded_schema)
expect_event_types_documented(loaded_schema)

validator = Draft202012Validator(loaded_schema, format_checker=FormatChecker())
expect_valid(validator, event, "provider unavailable friction event")

manual_step = copy.deepcopy(event)
manual_step["event_id"] = "55555555-5555-4555-8555-555555555555"
manual_step["friction_type"] = "repeated_manual_step"
manual_step["severity"] = "medium"
manual_step["provider"] = "local-shell"
manual_step.pop("evidence")
manual_step["summary"] = "Release packaging required repeated manual command checks."
manual_step["proposed_action"] = {
    "action_type": "backlog",
    "title": "Add release packaging preflight checklist",
}
expect_valid(validator, manual_step, "medium repeated manual step")

invalid = copy.deepcopy(event)
invalid["friction_type"] = "provider_unavailable"
invalid.pop("provider")
expect_invalid(validator, invalid, "provider unavailable without provider")

invalid = copy.deepcopy(event)
invalid["severity"] = "high"
invalid.pop("evidence")
expect_invalid(validator, invalid, "high severity without evidence")

invalid = copy.deepcopy(event)
invalid["friction_type"] = "made_up_type"
expect_invalid(validator, invalid, "unknown friction type")

print("Friction taxonomy schema and semantic fixtures passed.")

```

#### BE\scripts\verify-governance-report-schema.py

```python
#!/usr/bin/env python3
import copy
import json
import sys
from pathlib import Path

from jsonschema import Draft202012Validator, FormatChecker


ROOT = Path(__file__).resolve().parents[1]
SCHEMA = ROOT / "docs" / "schemas" / "governance-report.schema.json"
DOC = ROOT / "docs" / "GOVERNANCE_REPORT.md"
DECISION = ROOT / "docs" / "decisions" / "0012-governance-report-schema.md"
IDENTITY_DECISION = ROOT / "docs" / "decisions" / "0013-hi-os-sovereign-identity.md"


def expect_valid(validator, instance, label):
    errors = sorted(validator.iter_errors(instance), key=lambda error: list(error.path))
    if errors:
        raise AssertionError(f"{label} should be valid: {errors[0].message}")


def expect_invalid(validator, instance, label):
    if not list(validator.iter_errors(instance)):
        raise AssertionError(f"{label} should be invalid")


with SCHEMA.open(encoding="utf-8") as handle:
    schema = json.load(handle)
Draft202012Validator.check_schema(schema)
validator = Draft202012Validator(schema, format_checker=FormatChecker())

for path in [DOC, DECISION]:
    text = path.read_text(encoding="utf-8")
    for required in [
        "governance-report",
        "identity",
        "story",
        "release",
        "friction",
    ]:
        if required not in text:
            raise AssertionError(f"{required} missing from {path}")

identity_decision_text = IDENTITY_DECISION.read_text(encoding="utf-8")
for required in [
    "hios.toml",
    "identity",
    "release",
    "governance",
    "ntu254/Harness-Intelligence-OS",
]:
    if required not in identity_decision_text:
        raise AssertionError(f"{required} missing from {IDENTITY_DECISION}")

report = {
    "schema_version": "1.1.0",
    "artifact_type": "governance-report",
    "report_id": "77777777-7777-4777-8777-777777777777",
    "generated_at": "2026-06-07T00:00:00Z",
    "identity": {
        "product_name": "Harness Intelligence OS",
        "short_name": "HI-OS",
        "slug": "hios",
        "repository": "ntu254/Harness-Intelligence-OS",
        "default_release_origin": "ntu254/Harness-Intelligence-OS",
    },
    "repository": {
        "origin": "ntu254/Harness-Intelligence-OS",
        "commit": "cc1f5e9",
        "branch": "main",
    },
    "story_summary": {
        "total": 15,
        "implemented": 14,
        "in_progress": 1,
        "blocked": 0,
    },
    "gate_summary": {
        "pass": 14,
        "fail": 1,
        "not_run": 0,
    },
    "validation_summary": {
        "commands": [
            {
                "command": "cargo test --workspace",
                "result": "pass",
            }
        ]
    },
    "release_summary": {
        "latest_version": "0.5.0",
        "release_verify_result": "pass",
        "assets_checked": 10,
    },
    "friction_summary": {
        "events": 2,
        "high_severity": 1,
        "open_backlog_suggestions": 1,
        "open_rule_proposals": 1,
    },
    "maturity_summary": {
        "score": 85,
        "level": "trusted",
        "gate_pass_percent": 93,
        "validation_pass_percent": 100,
        "release_verified": True,
        "open_governance_gaps": 2,
        "notes": ["Release verification passed."],
    },
    "stories": [
        {
            "story_id": "US-033",
            "status": "implemented",
            "risk_lane": "high_risk",
            "proof": {
                "unit": True,
                "integration": True,
                "e2e": True,
                "platform": True,
            },
            "gate_result": "pass",
            "missing_evidence": [],
            "evidence": "release verify 0.5.0 pass",
        }
    ],
}

expect_valid(validator, report, "complete governance report")

invalid = copy.deepcopy(report)
invalid["artifact_type"] = "dashboard"
expect_invalid(validator, invalid, "wrong artifact type")

invalid = copy.deepcopy(report)
invalid.pop("identity")
expect_invalid(validator, invalid, "missing identity")

invalid = copy.deepcopy(report)
invalid["identity"]["default_release_origin"] = ""
expect_invalid(validator, invalid, "empty identity release origin")

invalid = copy.deepcopy(report)
invalid["release_summary"]["release_verify_result"] = "warning"
expect_invalid(validator, invalid, "invalid release result")

invalid = copy.deepcopy(report)
invalid["stories"][0]["gate_result"] = "inconclusive"
expect_invalid(validator, invalid, "story gate cannot be inconclusive")

invalid = copy.deepcopy(report)
invalid["validation_summary"]["commands"][0]["result"] = "warning"
expect_invalid(validator, invalid, "validation command cannot be warning")

invalid = copy.deepcopy(report)
invalid["story_summary"]["total"] = -1
expect_invalid(validator, invalid, "negative count")

invalid = copy.deepcopy(report)
invalid["maturity_summary"]["score"] = 101
expect_invalid(validator, invalid, "maturity score over 100")

invalid = copy.deepcopy(report)
invalid["maturity_summary"]["level"] = "perfect"
expect_invalid(validator, invalid, "invalid maturity level")

invalid = copy.deepcopy(report)
invalid["stories"][0]["risk_lane"] = "urgent"
expect_invalid(validator, invalid, "invalid risk lane")

invalid = copy.deepcopy(report)
invalid["unexpected"] = True
expect_invalid(validator, invalid, "additional root property")

invalid = copy.deepcopy(report)
invalid["stories"][0].pop("gate_result")
expect_invalid(validator, invalid, "missing gate result")

for report_path in sys.argv[1:]:
    with Path(report_path).open(encoding="utf-8") as handle:
        generated_report = json.load(handle)
    expect_valid(validator, generated_report, f"generated report {report_path}")

print("Governance report schema and semantic fixtures passed.")

```

#### BE\scripts\verify-mcp-artifact-contracts.py

```python
#!/usr/bin/env python3
import copy
import json
from pathlib import Path

from jsonschema import Draft202012Validator, FormatChecker


ROOT = Path(__file__).resolve().parents[1]
SCHEMA_DIR = ROOT / "docs" / "schemas"


def load_schema(name):
    with (SCHEMA_DIR / name).open(encoding="utf-8") as handle:
        schema = json.load(handle)
    Draft202012Validator.check_schema(schema)
    return Draft202012Validator(schema, format_checker=FormatChecker())


def expect_valid(validator, instance, label):
    errors = sorted(validator.iter_errors(instance), key=lambda error: list(error.path))
    if errors:
        raise AssertionError(f"{label} should be valid: {errors[0].message}")


def expect_invalid(validator, instance, label):
    if not list(validator.iter_errors(instance)):
        raise AssertionError(f"{label} should be invalid")


sha = "a" * 64
codegraph = {
    "schema_version": "1.0.0",
    "artifact_type": "codegraph-impact",
    "artifact_id": "11111111-1111-4111-8111-111111111111",
    "story_id": "US-023",
    "status": "pass",
    "generated_at": "2026-06-07T00:00:00Z",
    "provenance": {
        "provider": "codegraph",
        "adapter": "hios-codegraph-adapter",
        "adapter_version": "0.1.0",
        "invocation_id": "run-1",
        "repository": "ntu254/Harness-Intelligence-OS",
        "revision": "3fdc04c",
        "inputs": [{"uri": "git:HEAD", "sha256": sha}],
    },
    "impact": {
        "summary": "Defines contracts only.",
        "affected_files": [
            {
                "path": "docs/schemas/codegraph-impact.schema.json",
                "change_kind": "documentation",
                "reasons": ["New versioned contract"],
            }
        ],
        "risk_flags": ["public_contracts", "external_systems"],
        "claims": [
            {
                "claim_id": "CG-1",
                "statement": "The story changes an intelligence artifact contract.",
                "evidence_refs": ["docs/schemas/codegraph-impact.schema.json"],
            }
        ],
    },
}

notebooklm = {
    "schema_version": "1.0.0",
    "artifact_type": "notebooklm-brief",
    "artifact_id": "22222222-2222-4222-8222-222222222222",
    "story_id": "US-023",
    "status": "pass",
    "generated_at": "2026-06-07T00:00:00Z",
    "provenance": {
        "provider": "notebooklm",
        "adapter": "hios-notebooklm-adapter",
        "adapter_version": "0.1.0",
        "invocation_id": "run-2",
        "sources": [
            {
                "source_id": "SRC-1",
                "title": "Harness operating model",
                "uri": "docs/HARNESS.md",
                "sha256": sha,
                "retrieved_at": "2026-06-07T00:00:00Z",
            }
        ],
    },
    "brief": {
        "summary": "MCP tools produce files that Harness validates.",
        "constraints": ["Providers do not write directly to SQLite."],
        "open_questions": [],
        "affected_docs": ["docs/HARNESS.md"],
        "claims": [
            {
                "claim_id": "NL-1",
                "statement": "Harness owns durable ingestion.",
                "citations": [{"source_id": "SRC-1", "locator": "Durable Layer"}],
            }
        ],
    },
}

ingest = {
    "schema_version": "1.0.0",
    "artifact_type": "context-ingest-result",
    "ingest_id": "33333333-3333-4333-8333-333333333333",
    "story_id": "US-023",
    "source": "codegraph",
    "source_artifact": {
        "artifact_type": "codegraph-impact",
        "artifact_id": codegraph["artifact_id"],
        "schema_version": "1.0.0",
        "path": ".harness/artifacts/codegraph-impact.json",
        "sha256": sha,
    },
    "status": "pass",
    "checked_at": "2026-06-07T00:01:00Z",
    "mapped_context": {
        "risk_flags": ["public_contracts"],
        "affected_files": ["docs/schemas/codegraph-impact.schema.json"],
        "code_impact_summary": "Defines contracts only.",
        "claim_ids": ["CG-1"],
    },
    "governance": {
        "eligible_for_intake": True,
        "eligible_for_context_pack": True,
        "eligible_for_story_verify": True,
    },
}

validators = {
    "codegraph": load_schema("codegraph-impact.schema.json"),
    "notebooklm": load_schema("notebooklm-brief.schema.json"),
    "ingest": load_schema("context-ingest-result.schema.json"),
}

expect_valid(validators["codegraph"], codegraph, "passing CodeGraph artifact")
expect_valid(validators["notebooklm"], notebooklm, "passing NotebookLM artifact")
expect_valid(validators["ingest"], ingest, "passing ingest result")

failed_codegraph = copy.deepcopy(codegraph)
failed_codegraph["status"] = "fail"
failed_codegraph.pop("impact")
failed_codegraph["errors"] = [
    {"code": "GRAPH_QUERY_FAILED", "message": "Graph query failed.", "retryable": False}
]
expect_valid(validators["codegraph"], failed_codegraph, "failed CodeGraph artifact")

inconclusive_notebooklm = copy.deepcopy(notebooklm)
inconclusive_notebooklm["status"] = "inconclusive"
inconclusive_notebooklm.pop("brief")
inconclusive_notebooklm["provenance"]["sources"] = []
inconclusive_notebooklm["unavailable"] = {
    "reason": "provider_unavailable",
    "retryable": True,
}
expect_valid(
    validators["notebooklm"],
    inconclusive_notebooklm,
    "inconclusive NotebookLM artifact",
)

inconclusive_ingest = copy.deepcopy(ingest)
inconclusive_ingest["status"] = "inconclusive"
inconclusive_ingest.pop("mapped_context")
inconclusive_ingest["diagnostics"] = [
    {
        "code": "SOURCE_UNAVAILABLE",
        "message": "MCP source was unavailable.",
        "retryable": True,
    }
]
inconclusive_ingest["governance"] = {
    "eligible_for_intake": False,
    "eligible_for_context_pack": False,
    "eligible_for_story_verify": False,
}
expect_valid(validators["ingest"], inconclusive_ingest, "inconclusive ingest result")

invalid = copy.deepcopy(codegraph)
invalid["status"] = "inconclusive"
expect_invalid(validators["codegraph"], invalid, "inconclusive CodeGraph without unavailable")

invalid = copy.deepcopy(codegraph)
invalid.pop("provenance")
expect_invalid(validators["codegraph"], invalid, "CodeGraph without provenance")

invalid = copy.deepcopy(notebooklm)
invalid["brief"]["claims"][0]["citations"] = []
expect_invalid(validators["notebooklm"], invalid, "ungrounded NotebookLM claim")

invalid = copy.deepcopy(ingest)
invalid["status"] = "inconclusive"
expect_invalid(validators["ingest"], invalid, "inconclusive ingest marked governance eligible")

print("MCP artifact contract schemas and semantic fixtures passed.")

```

#### full-up.bat

```batch
@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\windows\full-up.ps1"
exit /b %ERRORLEVEL%

```

#### generate-rsa-keys.ps1

```powershell
# generate-rsa-keys.ps1
# Run from project root: .\generate-rsa-keys.ps1

# PREREQUISITE: If openssl is not recognized, add Git/usr/bin to PATH:
# $env:Path += ";C:\Program Files\Git\usr\bin"

$certsDir = ".docker\certs"

# Create directory if it doesn't exist
if (-not (Test-Path $certsDir)) {
    New-Item -ItemType Directory -Path $certsDir | Out-Null
}

# Add Git usr/bin to PATH if needed
$opensslPath = "C:\Program Files\Git\usr\bin\openssl.exe"
if (-not (Get-Command openssl -ErrorAction SilentlyContinue)) {
    if (Test-Path $opensslPath) {
        $env:Path += ";C:\Program Files\Git\usr\bin"
    } else {
        Write-Host "ERROR: OpenSSL not found. Please install Git for Windows or add OpenSSL to PATH."
        exit 1
    }
}

# Generate RSA-2048 Private Key using modern openssl genpkey
Write-Host "Generating RSA-2048 private key..."
& openssl genpkey -algorithm RSA -out "$certsDir\private_key.pem" -pkeyopt rsa_keygen_bits:2048

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to generate private key."
    exit 1
}

# Extract Public Key from Private Key
Write-Host "Extracting public key from private key..."
& openssl rsa -in "$certsDir\private_key.pem" -pubout -out "$certsDir\public_key.pem"

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to extract public key."
    exit 1
}

# Set appropriate permissions
Write-Host "Setting file permissions..."
icacls "$certsDir\private_key.pem" /inheritance:r | Out-Null
icacls "$certsDir\private_key.pem" /grant:r "$($env:USERNAME):(R)" | Out-Null

Write-Host "✅ RSA-2048 key pair generated successfully:"
Write-Host "   Private Key: $certsDir\private_key.pem"
Write-Host "   Public Key:  $certsDir\public_key.pem"

```

#### scripts\full-up.sh

```bash
#!/usr/bin/env sh
set -eu

repository_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
compose_file="$repository_root/BE/docker-compose.full.yml"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker CLI was not found. Install Docker Desktop or Docker Engine first." >&2
  exit 1
fi

if ! docker version >/dev/null 2>&1; then
  echo "Docker daemon is not running. Start Docker and retry." >&2
  exit 1
fi

echo "Starting the full StyleMind stack. Existing volumes will be preserved."
exec docker compose -f "$compose_file" up -d --build

```

#### scripts\windows\full-down.ps1

```powershell
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker CLI was not found. Install Docker Desktop and reopen the terminal.'
}

& docker version *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Desktop is not running. Start Docker Desktop, wait for it to become ready, then retry.'
}

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$composeFile = Join-Path $repositoryRoot 'BE\docker-compose.full.yml'

if (-not (Test-Path -LiteralPath $composeFile)) {
    throw "Compose file was not found: $composeFile"
}

Write-Host 'Stopping the StyleMind stack and removing orphan containers. Volumes are preserved.' -ForegroundColor Yellow
& docker compose -f $composeFile down --remove-orphans
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host 'StyleMind stack stopped. Named volumes were not removed.' -ForegroundColor Green

```

#### scripts\windows\full-up.ps1

```powershell
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

function Assert-DockerReady {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker CLI was not found. Install Docker Desktop and reopen the terminal.'
    }

    & docker version *> $null
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Desktop is not running. Start Docker Desktop, wait for it to become ready, then retry.'
    }
}

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$composeFile = Join-Path $repositoryRoot 'BE\docker-compose.full.yml'

if (-not (Test-Path -LiteralPath $composeFile)) {
    throw "Compose file was not found: $composeFile"
}

Assert-DockerReady

Write-Host 'Starting the full StyleMind stack. Existing volumes will be preserved.' -ForegroundColor Cyan
& docker compose -f $composeFile up -d --build
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host 'StyleMind stack started. Use scripts\windows\logs.ps1 -Follow to inspect logs.' -ForegroundColor Green

```

#### scripts\windows\logs.ps1

```powershell
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$Service,

    [switch]$Follow
)

$ErrorActionPreference = 'Stop'

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker CLI was not found. Install Docker Desktop and reopen the terminal.'
}

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$composeFile = Join-Path $repositoryRoot 'BE\docker-compose.full.yml'

if (-not (Test-Path -LiteralPath $composeFile)) {
    throw "Compose file was not found: $composeFile"
}

$arguments = @('compose', '-f', $composeFile, 'logs')
if ($Follow) {
    $arguments += '--follow'
}
if ($Service) {
    $arguments += $Service
}

& docker @arguments
exit $LASTEXITCODE

```


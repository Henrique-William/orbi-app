# Relatório de Refatoração de UI e Integração com a Orbi API

**Projeto:** Orbi App (Android)  
**Linguagem:** 100% Java  
**Status do Build:** `BUILD SUCCESSFUL` (Compilado e empacotado em APK com sucesso)  
**Base de Conhecimento:** `mobile-app-ui-design` e `docs/orbi-api/orbi-api.md`  

---

## 1. 🎨 Refatoração da UI (Baseado no Mobile App UI Design)

A interface de usuário foi completamente aprimorada para seguir os padrões visuais e ergonômicos de aplicativos mobile de classe mundial:

### 1.1. Sistema de Cores (Regra 60/30/10)
- **60% (Base Neutra):** Fundo suave em tom cinza claro (`#F8F9FA`) e cards brancos puros (`#FFFFFF`).
- **30% (Estrutura e Leitura):** Tipografia escura de alto contraste (`#1F2937` para títulos, `#4B5563` para subtítulos e `#E5E7EB` para linhas divisórias).
- **10% (Cor de Destaque / Ação):** Azul elétrico Orbi (`#1A73E8`) com áreas tonais em pílula (`#E8F0FE`). Cores de semáforo aplicadas estritamente com fundos suaves (Verde `#E6F4EA` para concluído, Âmbar `#FEF7E0` para pulado, Coral `#FCE8E6` para cancelado).

### 1.2. Grid de 8 Pontos e Hierarquia Visual
- Todos os paddings e margens seguem estritamente múltiplos de 8 ou 4 dp (`8dp`, `16dp`, `24dp`, `32dp`).
- Números e métricas de desempenho ganharam destaque de tipografia (ex: `4 paradas`, `~45 min`, código `ROTA-SP-01` em 22sp negrito).
- Cards com cantos arredondados modernos de `24dp` de raio e elevação sutil com borda fina de 1dp.

### 1.3. Timeline Vertical em Detalhes da Rota
- Substituição da lista estática por um componente de **Linha do Tempo Visual (Timeline Tracking)**:
  - Círculo de passo numerado (`#01`, `#02`...) com cores dinâmicas conforme o status.
  - Linha vertical conectando uma parada à outra, oculta no último item.
  - Sensação de fluxo operacional contínuo (padrão de apps como Uber, DoorDash e Amazon Logistics).

### 1.4. Ergonomia e "Thumb Zone"
- Todos os botões primários de ação ("Acessar Rota", "Iniciar Rota", "Concluir Endereço", "Pular", "Cancelar") posicionados no terço inferior da tela com alvos de toque amplos (mínimo de 52dp de altura).

---

## 2. 🔌 Integração com a Orbi API (docs/orbi-api/orbi-api.md)

### 2.1. Arquitetura de Rede (Retrofit 2 + OkHttp 3 + Gson)
- **Cliente Centralizado (`ApiClient.java`):**
  - Configurado com Base URL padrão `http://10.0.2.2:8080/` (alias do emulador para o host local).
  - Interceptor de autenticação que injeta automaticamente o cabeçalho `Authorization: Bearer <accessToken>`.
  - Logging interceptor ativado para depuração completa de payloads HTTP.
  - Suporte a `android:usesCleartextTraffic="true"` no Manifesto para conexões locais HTTP.

### 2.2. Endpoints Mapeados e Integrados
| Endpoint Orbi API | Método | Classe Responsável | Ação no App |
| :--- | :--- | :--- | :--- |
| `/api/auth/login` | `POST` | `LoginActivity.java` | Autenticação do motorista e captura do JWT. |
| `/api/route` | `GET` | `HomeActivity.java` | Listagem em tempo real das rotas com entregas. |
| `/api/route/{id}/directions` | `GET` | `NavigationMapActivity.java` | Traçado GeoJSON e instruções turn-by-turn do OpenRouteService. |
| `/api/deliveries/{id}/next-status` | `PATCH` | `NavigationMapActivity.java` | Avança o status da entrega (`IN_TRANSIT` ➔ `DELIVERED`). |
| `/api/deliveries/{id}/cancel` | `PATCH` | `NavigationMapActivity.java` | Cancela entrega na API com justificativa. |

### 2.3. Resiliência e Offline-First
- Caso o servidor local Spring Boot esteja temporariamente desligado, o `RouteRepository` aplica fallback automático e transparente para a base local em memória/cache, permitindo que a navegação e os testes da interface continuem operando sem travamentos.

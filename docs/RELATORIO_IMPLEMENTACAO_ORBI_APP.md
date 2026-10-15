# Relatório Técnico de Implementação - Orbi App (Android)

**Projeto:** Orbi App (Navegação & Gestão Logística para Motoristas)  
**Linguagem:** 100% Java  
**Status do Build:** `BUILD SUCCESSFUL` (APK Debug gerado e validado)  
**Versão:** 1.0  

---

## 1. 📋 Resumo Executivo da Implementação

Todas as 4 telas e seus respectivos fluxos operacionais foram desenvolvidos estritamente em **Java**, com arquitetura modular e desacoplada, utilizando **Material Design 3**, componentes customizados de navegação em tempo real e persistência local:

1. **Tela de Login (`LoginActivity`):**
   - Autenticação e validação prévia de e-mail e senha.
   - Gerenciamento de sessão com `SessionManager` (`SharedPreferences`).
   - Lógica de auto-login: motoristas com sessão ativa pulam automaticamente a tela de login.
   - Indicador de loading integrado ao botão de ação e tratamento de erros.

2. **Tela de Rotas / Home (`HomeActivity`):**
   - Lista horizontal infinita/contínua de rotas atribuídas (`RecyclerView` horizontal com `PagerSnapHelper` para centralização suave de cards).
   - Barra de badges/chips superiores flutuantes para filtragem dinâmica:
     - `Todas`
     - `A fazer` (`PENDING`)
     - `Atrasado` (`DELAYED`)
     - `Em andamento` (`IN_PROGRESS`)
     - `Concluídas` (`COMPLETED`)
   - **Regra de Negócio Crucial Implementada:** Rotas concluídas ficam ocultas por padrão na listagem comum e só passam a ser exibidas quando o badge **"Concluídas"** for expressamente selecionado.
   - Indicadores visuais de total de paradas, tempo estimado e barra de progresso em cada card.
   - Opção de encerramento seguro de sessão (Logout) com diálogo de confirmação.

3. **Tela de Detalhes da Rota (`RouteDetailsActivity`):**
   - Cabeçalho com código da rota, status e resumo de entregas concluídas.
   - Listagem vertical de paradas na ordem estrita de entrega (`sequenceOrder: 1, 2, 3...`).
   - Exibição de dados completos do destinatário, logradouro, telefone e motivos de pulo/cancelamento quando existentes.
   - **Botão Dinâmico de Ação:**
     - Exibe **"Iniciar Rota"** (em azul) enquanto houver paradas pendentes, acionando o mapa.
     - Alterna automaticamente para **"Concluir Rota"** (em verde) assim que todas as paradas forem tratadas (concluídas, puladas ou canceladas), finalizando a rota no repositório e retornando à Home.

4. **Tela do Mapa de Navegação em Tempo Real (`NavigationMapActivity`):**
   - Componente visual de mapa nativo vetorial minimalista (`RealTimeMapView`):
     - Vias urbanas, avenidas principais e quarteirões estilizados no padrão visual do Google Maps.
     - Traçado vetorial (Polyline) em tempo real da localização atual do motorista até a próxima parada pendente.
     - Marcador do motorista com indicador direcional (*bearing*) e anel de pulso do GPS.
     - Marcador de destino com numeração correspondente à ordem da entrega.
     - Botão flutuante para recentralizar o mapa no veículo.
   - Painel superior compacto com nome da rua, destinatário e estimativa de chegada (ETA / distância).
   - **Controles Flutuantes da Condução:**
     - **Concluir Endereço:** Marca a parada como concluída e avança automaticamente para a próxima parada da ordem, recalculando a rota.
     - **Pular Endereço:** Abre diálogo modal com seleção obrigatória de justificativa (*Destinatário Ausente*, *Via Bloqueada*, *Estabelecimento Fechado*, *Outro*), atualiza status para `Pulado` e recalcula a rota imediatamente para o próximo ponto.
     - **Cancelar Endereço:** Abre diálogo modal com justificativa obrigatória (*Cancelado pelo Cliente*, *Endereço Inexistente*, *Carga Avariada*, *Outro*), marca como `Cancelado` e traça o caminho para o endereço subsequente.
     - Diálogo de encerramento automático quando todas as paradas forem finalizadas.

---

## 2. 🏗 Arquivos e Estrutura Criada

```
app/src/main/
├── AndroidManifest.xml
├── java/com/orbi/orbi_app/
│   ├── model/
│   │   ├── Route.java
│   │   ├── RouteStatus.java
│   │   ├── Waypoint.java
│   │   └── WaypointStatus.java
│   ├── data/
│   │   ├── SessionManager.java
│   │   └── RouteRepository.java
│   └── ui/
│       ├── login/
│       │   └── LoginActivity.java
│       ├── home/
│       │   ├── HomeActivity.java
│       │   └── RouteCarouselAdapter.java
│       ├── details/
│       │   ├── RouteDetailsActivity.java
│       │   └── WaypointAdapter.java
│       └── map/
│           ├── NavigationMapActivity.java
│           └── RealTimeMapView.java
└── res/
    ├── drawable/
    │   ├── bg_card_rounded.xml
    │   ├── bg_badge_rounded.xml
    │   ├── ic_check.xml
    │   ├── ic_skip.xml
    │   ├── ic_cancel.xml
    │   ├── ic_my_location.xml
    │   ├── ic_arrow_back.xml
    │   └── ic_logout.xml
    ├── layout/
    │   ├── activity_login.xml
    │   ├── activity_home.xml
    │   ├── item_route_card.xml
    │   ├── activity_route_details.xml
    │   ├── item_waypoint.xml
    │   ├── activity_navigation_map.xml
    │   └── dialog_justification.xml
    └── values/
        ├── colors.xml
        └── strings.xml
```

---

## 3. 🚀 Próximos Passos Recomendados

Para dar sequência à evolução do projeto e conectar a aplicação ao ambiente real de produção, sugerem-se as seguintes ações:

1. **Configuração da Chave do Google Maps SDK (Produção):**
   - Obter uma API Key no console do Google Cloud Platform habilitando o *Maps SDK for Android* e o *Directions API*.
   - Inserir a meta-data no `AndroidManifest.xml` caso deseje alternar entre o `RealTimeMapView` nativo e o `com.google.android.gms.maps.MapView`.

2. **Integração Real com a Orbi API (Retrofit):**
   - Adicionar as dependências do `Retrofit 2` e `OkHttp 3` no `build.gradle.kts`.
   - Implementar os endpoints REST da **Orbi API** em Java:
     - `POST /api/auth/login` (Obtenção do token JWT).
     - `GET /api/routes/assigned` (Carga das rotas do motorista).
     - `PATCH /api/routes/{id}/status` (Atualização de status da rota).
     - `PATCH /api/deliveries/{id}/status` (Concluir, pular ou cancelar parada com motivo).

3. **Foreground Service de Localização Real (`FusedLocationProviderClient`):**
   - Criar `LocationTrackingService.java` herdando de `Service` com notificação persistente para leitura de GPS em tempo real mesmo com a tela do celular desligada.

4. **Persistência Local Offline-First (Room Database):**
   - Migrar os dados em memória do `RouteRepository` para tabelas SQLite via **Android Room**, garantindo funcionamento 100% contínuo mesmo em túneis ou áreas sem sinal 4G/5G.

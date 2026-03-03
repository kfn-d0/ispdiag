# ISP Diagnostic

Aplicativo para diagnóstico de conectividade, projetado para cenários onde um cliente de provedor de internet relata não conseguir acessar determinados serviços. Executa testes camada por camada (DNS → TCP → TLS → HTTP), coleta informações do ambiente de rede e gera relatórios técnicos exportáveis.

![Screenshot_2026-03-03-12-00-20-009_com ispdiag](https://github.com/user-attachments/assets/694e14e2-091b-4bee-a5fa-f11fffc1ce87)

**Execução 100% local** — nenhum backend, nenhuma captura de tráfego de terceiros.

---

## Funcionalidades

### Serviços Pré-Configurados

| Serviço | URL |
|---------|-----|
| Google | `google.com` |
| YouTube | `youtube.com` |
| TikTok | `tiktok.com` |
| Twitch | `twitch.tv` |
| WhatsApp | `whatsapp.com` |
| Instagram | `instagram.com` |
| Steam | `store.steampowered.com` |
| Netflix | `netflix.com` |
| Globo | `globo.com` |
| Shopee | `shopee.com.br` |
| Mercado Livre | `mercadolivre.com.br` |

O usuário pode selecionar serviços individuais, marcar todos ou adicionar domínios customizados.

### Testes Diagnósticos (Layer by Layer)

Cada serviço selecionado passa por uma pilha completa de testes, executados sequencialmente:

| Camada | Teste | Dados Coletados |
|--------|-------|-----------------|
| **DNS** | Resolução de domínio | Tempo de resposta, endereços IPv4/IPv6, erro NXDOMAIN, timeout |
| **TCP** | Conexão porta 80 e 443 | Tempo de establish, RST, connection refused, unreachable |
| **TLS** | Handshake TLS | Versão TLS (1.2/1.3), cipher suite, certificado (subject, issuer, validade), SNI |
| **HTTP** | Requisição GET com TTFB | Status code, tempo total, redirects, TTFB (3 iterações: min/max/média) |
| **Traceroute** | Rastreamento de rota | Saltos intermediários (IP, hostname, latência por salto), detecção de destino |

#### TTFB (Time To First Byte)

O teste HTTP executa **3 iterações** no endpoint final (após seguir redirects) e mede o tempo entre o envio da requisição e o recebimento dos primeiros headers de resposta. Isso captura o **tempo de processamento do servidor** (backend, CDN, WAF), não a latência de transporte.

Resultados exibidos:
- TTFB médio, mínimo e máximo
- Número de iterações

#### Comparação DNS

Para cada serviço, o aplicativo compara o tempo de resolução DNS entre múltiplos resolvers:

| Resolver | Servidor |
|----------|----------|
| Sistema | DNS configurado no dispositivo (via raw UDP) |
| Google | `8.8.8.8` |
| Cloudflare | `1.1.1.1` |
| Quad9 | `9.9.9.9` |
| OpenDNS | `208.67.222.222` |

O DNS do sistema é medido via query UDP direta ao servidor obtido de `LinkProperties`, evitando cache do `InetAddress.getAllByName()`.

### Testes Avançados (Opcionais)

Habilitados via checkbox na tela principal:

| Teste | Descrição |
|-------|-----------|
| **MTU Detection** | Descobre o MTU efetivo do path usando busca binária com pacotes `ping -s` |
| **SNI Blocking** | Detecta bloqueio de SNI comparando handshakes TLS com e sem extensão SNI |
| **Proxy/MITM** | Identifica proxies transparentes ou interceptação TLS comparando certificados esperados vs recebidos |

### Seleção de Protocolo IP

Checkbox para **forçar IPv6**. Por padrão, o aplicativo usa IPv4. Com a opção marcada:
- `java.net.preferIPv6Addresses` é setado como `true`
- Todas as resoluções DNS, conexões TCP, handshakes TLS e requisições HTTP preferem endereços IPv6
- As configurações são restauradas após a execução dos testes

### Informações de Ambiente

Coletadas automaticamente antes dos testes:

| Info | Fonte |
|------|-------|
| Tipo de conexão | Wi-Fi / 4G / 5G / Ethernet |
| Gateway IPv4 | Default route via `RouteInfo` (filtrado para `Inet4Address`) |
| IP local | Endereço da interface ativa |
| IP público IPv4 | Via `https://ifconfig.me/ip` |
| IPv6 | Disponibilidade e endereço público IPv6 |
| Servidores DNS | Lista de DNS configurados no dispositivo |

### Gráfico de Latência por Camada

Visualização customizada (`LatencyChartView`) que exibe cada camada (DNS, TCP, TLS, HTTP) como uma **linha separada** com barra proporcional e valor em milissegundos. Cada serviço tem sua própria seção no gráfico, permitindo identificar rapidamente qual camada é o bottleneck.

### Histórico de Diagnósticos

- Armazenamento local via SQLite (`DiagnosticDatabase`)
- Cada execução salva: timestamp, quantidade de serviços, status (OK/Parcial/Falha), relatório JSON completo
- Visualização do histórico com estatísticas resumidas
- Opção de limpar histórico

### Relatório Técnico (com Inferência Automática)

O relatório padrão é um **resumo técnico legível** com conclusões automáticas.

#### Vista padrão: Relatório Técnico

```
==== EXEMPLO DIAGNOSTICO TECNICO ====

>> Geral
  [~] IPv6 indisponivel nesta rede

>> Mercado Livre
  [ok] DNS OK (15ms)
  [ok] TCP:443 OK (14ms)
  [ok] TLS OK (52ms) TLSv1.3
  [ok] HTTP OK (780ms) [200]
  [~] TTFB alto: 710ms (servidor: ~696ms)

  Conclusao:
  - Transporte rapido (TCP 14ms) mas TTFB alto (710ms) - processamento server-side lento, nao e problema do ISP
    Confianca: Alta

>> TikTok
  [!] DNS falhou: NXDOMAIN
  [!] DNS sistema falhou
  [ok] DNS Google resolve

  Conclusao:
  - Resolver retornou NXDOMAIN - dominio nao encontrado ou bloqueado
    Confianca: Alta
  - Possivel bloqueio no resolver do ISP
    Confianca: Alta
```

#### Vista alternativa: JSON estruturado

Toggle via botão **"JSON"** no header. O JSON completo contém todos os dados brutos para integração com sistemas de gestão.

### Motor de Inferência Automática

O `DiagnosticInference` analisa os resultados e gera conclusões estruturadas:

| Cenário Detectado | Conclusão | Confiança |
|---|---|---|
| DNS NXDOMAIN | Bloqueio na resolução | Alta |
| DNS timeout | Resolução lento/inacessível | Média |
| DNS ISP > 200ms, Google < 50ms | Resolução ISP sobrecarregado | Média |
| DNS sistema falha + Google resolve | Bloqueio no ISP | Alta |
| TCP:443 RST/refused | Firewall ou serviço indisponível | Alta |
| TCP:443 timeout | Porta filtrada ou host down | Média |
| TLS handshake_failure | DPI/proxy interceptando | Alta |
| TLS ≥ 3× TCP baseline | Handshake delay suspeito | Média |
| TTFB alto + TCP rápido | Backend lento, não é ISP | Alta |
| TTFB alto + TCP lento | Verificar CDN edge/origin | Média |
| SNI bloqueado | DPI filtrando por hostname | Alta |
| Proxy/MITM detectado | Interceptação TLS | Alta |
| Todos falham em DNS | Problema no resolver ISP | Alta |
| Todos falham em TCP | Queda total ou bloqueio | Alta |
| Todos falham em TLS | DPI/proxy em toda rede | Alta |

---

## Arquitetura

```
app/
├── AndroidManifest.xml
├── res/
│   ├── drawable/              # Backgrounds, badges, gradientes
│   ├── layout/
│   │   ├── activity_main.xml      # Seleção de serviços + opções
│   │   ├── activity_result.xml    # Resultados com gráfico
│   │   ├── activity_detail.xml    # Detalhes técnicos
│   │   ├── activity_report.xml    # Relatório JSON
│   │   ├── activity_history.xml   # Histórico
│   │   ├── item_service.xml       # Item da lista de serviços
│   │   ├── item_result.xml        # Card de resultado por serviço
│   │   └── item_history.xml       # Item do histórico
│   └── values/
│       ├── colors.xml             # Paleta (tema claro, tons de azul)
│       ├── strings.xml            # Strings localizáveis
│       └── styles.xml             # Tema Material Light
│
└── src/com/ispdiag/
    ├── MainActivity.java          # Tela principal (seleção + execução)
    ├── ResultActivity.java        # Resultados + gráfico + DNS comparison
    ├── DetailActivity.java        # Detalhes técnicos por serviço
    ├── ReportActivity.java        # Relatório JSON exportável
    ├── HistoryActivity.java       # Histórico de diagnósticos
    │
    ├── diagnostic/
    │   ├── DiagnosticRunner.java       # Orquestrador (IPv4/IPv6, progress)
    │   ├── DnsTest.java               # Resolução DNS (IPv4 + IPv6)
    │   ├── TcpTest.java               # Conexão TCP (porta 80/443)
    │   ├── TlsTest.java               # Handshake TLS + certificado
    │   ├── HttpTest.java              # HTTP GET + TTFB (3 iterações)
    │   ├── TracerouteTest.java        # Traceroute via ping TTL
    │   ├── DnsComparisonTest.java     # Comparação multi-resolver
    │   ├── AdvancedTests.java         # MTU, SNI blocking, proxy
    │   └── EnvironmentCollector.java  # Coleta de info do ambiente
    │
    ├── model/
    │   ├── ServiceTarget.java         # Definição de serviço (nome + URL)
    │   ├── DiagResult.java            # Resultado completo por serviço
    │   ├── DnsResult.java             # Resultado DNS
    │   ├── TcpResult.java             # Resultado TCP
    │   ├── TlsResult.java             # Resultado TLS
    │   ├── HttpResult.java            # Resultado HTTP + TTFB
    │   ├── TracerouteResult.java      # Resultado traceroute
    │   ├── TracerouteHop.java         # Salto individual
    │   ├── DnsComparisonEntry.java    # Entrada de comparação DNS
    │   ├── AdvancedResult.java        # Resultado testes avançados
    │   └── EnvironmentInfo.java       # Info do ambiente de rede
    │
    ├── view/
    │   └── LatencyChartView.java      # Gráfico customizado de latência
    │
    └── util/
        ├── JsonBuilder.java           # Gerador de relatório JSON/texto
        ├── DiagnosticInference.java   # Motor de inferência automática
        └── DiagnosticDatabase.java    # SQLite para histórico
```

### Fluxo de Execução

```
MainActivity (seleção)
    │
    ├── DiagnosticRunner.runDiagnostics()
    │   ├── Set IPv4/IPv6 preference
    │   ├── EnvironmentCollector.collect()
    │   └── Para cada serviço:
    │       ├── DnsTest.execute(host)
    │       ├── TcpTest.execute(ip, 80)
    │       ├── TcpTest.execute(ip, 443)
    │       ├── TlsTest.execute(host)
    │       ├── HttpTest.execute(url)        ← 3x TTFB
    │       ├── DnsComparisonTest.compare(host)
    │       ├── TracerouteTest.execute(host)
    │       └── [Opcional] AdvancedTests.runAll(host)
    │
    ├── ResultActivity (visão geral + gráfico)
    │   ├── LatencyChartView (DNS/TCP/TLS/HTTP por serviço)
    │   └── DNS Comparison table
    │
    ├── DetailActivity (detalhes técnicos por serviço)
    │   ├── DNS, TCP:80, TCP:443, TLS, HTTP, TTFB
    │   ├── Traceroute (salto a salto)
    │   └── [Opcional] MTU, SNI, Proxy/MITM
    │
    └── ReportActivity (JSON exportável)
```

---

## UI / Design

- **Tema claro** com tons de azul (`Material.Light.NoActionBar`)
- Header com gradiente azul escuro (`#0D47A1` → `#1565C0`)
- Cards brancos com bordas sutis (radius 2-4dp)
- Paleta de status: verde (OK), laranja (parcial), vermelho (falha)
- Barras de latência com cores por camada: azul (DNS), verde (TCP), laranja (TLS), vermelho (HTTP)
- Badges de status com fundo translúcido

---

## Build (Manual sem Gradle)

Este projeto **não usa Gradle**. A compilação é feita via script PowerShell que chama diretamente as ferramentas do Android SDK.

### Pré-requisitos

| Requisito | Versão |
|-----------|--------|
| Android SDK | Build-tools 36.x, Platform android-36 |
| JDK | 11+ (Eclipse Adoptium recomendado) |
| PowerShell | 5.1+ (Windows) |

### Compilar

```powershell
.\scripts\build.ps1 -Clean
```

### Compilar e instalar

```powershell
.\scripts\build.ps1 -Clean -Install
```

O APK é gerado em `ISPDiagnostic.apk` na raiz do projeto.

### Pipeline de Build

1. **AAPT2 compile** — Compila recursos XML
2. **AAPT2 link** — Gera `R.java` e vincula recursos
3. **javac** — Compila fontes Java (source 8)
4. **d8** — Converte bytecode para DEX
5. **AAPT2 package** — Empacota APK
6. **zipalign** — Alinha ZIP para otimização
7. **apksigner** — Assina APK com keystore de debug

---

## Permissões

| Permissão | Finalidade |
|-----------|-----------|
| `INTERNET` | Executar testes de conectividade (DNS, TCP, TLS, HTTP) |
| `ACCESS_NETWORK_STATE` | Detectar tipo de conexão e gateway |
| `ACCESS_WIFI_STATE` | Obter informações de rede Wi-Fi |

---

## Requisitos Não Funcionais

- ✅ Nenhuma captura de tráfego de outros apps
- ✅ Nenhum uso de VPNService
- ✅ Nenhum sniffing de pacotes
- ✅ Nenhum backend obrigatório
- ✅ Execução 100% local no dispositivo
- ✅ Código modular e extensível
- ✅ Sem dependências externas (apenas Android SDK)
- ✅ Compatível com Android 8.0+ (API 26)
- ✅ Target SDK: Android 14 (API 34)

---

## Limitações Técnicas Conhecidas

### Execução sequencial com bloqueio por falha estrutural

Os testes são executados em cascata: **DNS → TCP → TLS → HTTP**. Se uma camada falha, as seguintes **não são executadas**:

- DNS falha → TCP, TLS, HTTP são **omitidos** (não geram ruído)
- TCP 443 falha → TLS e HTTP são **omitidos**
- TLS falha → HTTP é **omitido**

Isso evita mascarar a causa raiz. Se DNS demora muito, apenas DNS é reportado como problema — os tempos de TCP/TLS/HTTP não são contaminados.

### TTFB: medição e limitações

O TTFB é medido com **3 iterações independentes** usando:
- `Connection: close` (impede reuso de socket)
- `setUseCaches(false)` (sem cache HTTP)
- Sleep de 100ms entre iterações (garante cold connection)
- `disconnect()` explícito em bloco `finally`

**O que o TTFB captura**: tempo desde o envio da request até o recebimento dos primeiros headers de resposta. Isso inclui DNS re-resolve + TCP + TLS + processamento servidor.

**O que o TTFB NÃO diferencia isoladamente**:
- Latência de transporte vs. backend lento
- Congestionamento vs. throttling
- CDN edge vs. origin server

Para uma análise mais precisa, o TTFB deve ser **interpretado em conjunto** com os tempos de TCP e TLS do mesmo teste. Se TCP = 15ms e TTFB = 400ms, o delta (~385ms) é processamento server-side.

### Traceroute em Android

O traceroute é implementado via `ping` com TTL incremental. Essa abordagem tem limitações:

- ICMP pode ser **rate-limited** por equipamentos intermediários
- Backbone pode **filtrar** ICMP (hops aparecem como `*`)
- O retorno pode ser **assimétrico** (resposta vem por caminho diferente)
- Firewalls podem bloquear completamente

O relatório inclui a nota: *"Traceroute via ICMP pode ser incompleto por política de rede"*.

---

## Cenário Real de Uso (ISP)

### Fluxo de suporte técnico

```
1. Cliente liga reclamando: "Não consigo acessar o Mercado Livre"
2. Técnico abre o app no celular do cliente (conectado na rede do provedor)
3. Seleciona Mercado Livre + Google + outros serviços de referência
4. Executa o diagnóstico (~30 segundos)
5. Analisa o relatório:
   - Se APENAS Mercado Livre falha → problema no destino ou rota específica
   - Se TODOS falham em DNS → problema no DNS do provedor
   - Se TODOS falham em TCP → possível bloqueio ou queda total
   - Se Google funciona mas ML não → rota específica com problema
6. Exporta o relatório JSON e envia para a equipe de NOC
```

### Quando usar

- **Chamado de cliente**: primeiro diagnóstico antes de escalar
- **Validação pós-manutenção**: confirmar que o serviço voltou
- **Comparação de rede**: testar em Wi-Fi vs 4G para isolar o lado do problema
- **Disputa com upstream**: gerar evidência técnica de que o problema não é na rede local

---

## Exemplo de Falha Interpretada

### Caso 1: DNS bloqueado para um serviço específico

```
Google       → DNS: OK (12ms) | TCP: OK | TLS: OK | HTTP: OK
TikTok       → DNS: FALHA (NXDOMAIN)
Instagram    → DNS: OK (15ms) | TCP: OK | TLS: OK | HTTP: OK
```

**Interpretação**: O DNS do provedor está retornando NXDOMAIN para TikTok. Possíveis causas:
- Bloqueio judicial aplicado no resolver do ISP
- Cache DNS envenenado/corrompido
- **Ação**: Confirmar na seção DNS Comparison se Google/Cloudflare resolvem normalmente

### Caso 2: TLS falha para todos os serviços

```
Google       → DNS: OK | TCP:443: OK (18ms) | TLS: FALHA (handshake_failure)
Netflix      → DNS: OK | TCP:443: OK (22ms) | TLS: FALHA (handshake_failure)
WhatsApp     → DNS: OK | TCP:443: OK (15ms) | TLS: FALHA (handshake_failure)
```

**Interpretação**: TCP funciona em todas as portas 443, mas o handshake TLS falha sistematicamente. Possíveis causas:
- Proxy transparente ou firewall interceptando TLS
- Dispositivo de DPI (Deep Packet Inspection) mal configurado
- **Ação**: Executar teste avançado de Proxy/MITM Detection

### Caso 3: HTTP lento com TCP rápido

```
Mercado Livre → DNS: 10ms | TCP: 14ms | TLS: 45ms | HTTP: 850ms
               TTFB medio: 780ms | min: 720ms | max: 850ms
```

**Interpretação**: Transporte está rápido (TCP 14ms). O bottleneck é o servidor:
- TTFB alto e consistente (~780ms) = processamento backend/CDN lento
- Não é problema do ISP
- **Ação**: Comparar mesmos números em rede 4G. Se TTFB é similar, confirma problema no destino

---

## Exemplo de Relatório Analisado

```
Ambiente:
  Conexao: Wi-Fi
  Gateway IPv4: 192.168.1.1
  IP Local: 192.168.1.105
  IP Publico: 100.72.34.12     ← CGNAT detectado (RFC 6598)
  IPv6: Nao disponivel
  DNS: 100.72.0.1, 100.72.0.2

Resultados:
  Google (google.com)          → OK
    DNS: 8ms | TCP:80: 12ms | TCP:443: 11ms | TLS: 28ms | HTTP: 95ms
    TTFB: avg 85ms, min 78ms, max 95ms [3x]

  Mercado Livre (mercadolivre.com.br) → PARCIAL
    DNS: 15ms | TCP:80: 18ms | TCP:443: 16ms | TLS: 52ms | HTTP: 780ms
    TTFB: avg 710ms, min 680ms, max 780ms [3x]

  TikTok (tiktok.com)          → FALHA
    DNS: FALHA (NXDOMAIN)
    TCP/TLS/HTTP: não executados (falha estrutural em DNS)

Analise:
  • Google OK: rede estável, rota funcionando
  • ML PARCIAL: transporte OK, TTFB alto = servidor/CDN lento (não é ISP)
  • TikTok FALHA: DNS retornando NXDOMAIN = bloqueio no resolver
    → Confirmar via DNS Comparison (Google 8.8.8.8 resolve? Sim → bloqueio no ISP)
  • CGNAT detectado: IP público 100.72.x.x está em range RFC 6598
  • IPv6 não disponível nesta rede
```

---

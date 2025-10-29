# API RESTful - Trabalho Final: SerraBucks
<span style="font-size: 2px;">Serratec - Residência TIC/ Software 2025.2 - Prof.: Alberto Paz.</span> 
<br><small>  Grupo 4: Gelverson Tiago, Giselle Garcia, Hyago Guimarães, Jessica Lima, Lucas Perrin e Natália Siqueira.</small>
<br>

<span> O **SerraBucks, e-Commerce de Cafés**, é uma API RESTful completa desenvolvida em Java com Spring Boot, projetada para gerenciar todo o ciclo de vida de vendas de uma loja de comércio varejista. A aplicação utiliza o padrão em camadas e implementa CRUDs robustos para entidades centrais como `Cliente`, `Produto` e `Categoria`. O sistema de pedidos (carrinho) é totalmente funcional, suportando relacionamentos N:N via `ItemPedido` e gerenciamento de estoque, além de consumir o serviço externo do ViaCEP para preenchimento automático de endereço do cliente. Como recurso de valor agregado, a API possui um sistema de **Cashback baseado em cupons** que permite ao cliente gerar crédito em todas as compras e utilizá-lo integralmente em pedidos futuros, com regras de vencimento controladas por tarefas agendadas. A segurança é gerenciada por Spring Security com JWT, e a API conta com tratamento de exceções customizado e documentação via Swagger/OpenAPI.</span>

# Funcionalidades Extras (Partes Individuais)
### 1.💰 Sistema de Cashback (Giselle Garcia)
O sistema de Cashback implementado tem o objetivo de fidelizar o cliente, gerando um valor de crédito em todas as compras e permitindo que o saldo acumulado seja utilizado integralmente como abatimento em pedidos futuros.

1. Regra de Negócio:<br>
* O Cashback é tratado como uma coleção de cupons/transações.
* Acúmulo (Carteira): O valor de todos os cupons isActive = true é somado no campo carteira da entidade Cliente.
* Geração: Toda compra paga gera um novo registro de Cashback, de 5% do valor final do pedido (após todos os descontos, incluindo o uso de Cashback anterior), e o valor é somado na carteira do cliente. O vencimento é de 30 dias após a criação.
* Uso Integral: O cliente usa o valor total de sua carteira. Após o uso, o PedidoService chama o CashbackService para desativar (isActive = false) os cupons utilizados.
* Vencimento Agendado: Há uma task diária (@Scheduled) que verifica se o cashback está a vencer (em 1 dia) ou expirados. Para cupons de cashback a vencer e a expirar (isActive = false).
* O cliente recebe um e-mail (pelo emailService) avisando sobre o vencimento desses cashbacks. Exemplo:
  <img width="372,5" height="168" alt="image" src="https://github.com/user-attachments/assets/e52462dc-305a-4d3f-a591-c2591f9f247f" />
  

* Assim que uma compra é concluída, com ou sem o cashback, um email é enviado para o cliente informando o valor que ele possui na "carteira". Exemplo:
* <img width="362,5" height="232,5" alt="image" src="https://github.com/user-attachments/assets/abf33ac6-fa11-4fb2-b437-1bbe0174c03e" />


2. Fluxo de Serviço (.service)<br>
A. CashbackService.java

| Método | Função |
| :--- | :--- |
| `buscarPorClienteId(Long clienteId)` | **[Read]** Busca e retorna a lista de todos os registros de `Cashback` com `isActive=true`, ordenados por `DataVencimento` (Decrescente). |
| `adicionar(Long clienteId, BigDecimal valor)` | **[Admin/Teste]** Cria um novo registro de `Cashback` com o valor fornecido e o salva no banco. Não atualiza a carteira do cliente. *Endpoint exposto no `CashbackController`*. |
| `ganharCashback(Cliente cliente, BigDecimal totalDoPedido)` | Chamado pelo `PedidoService` após o pagamento. Cria um novo registro de `Cashback` (cupom), calcula 5% do `totalDoPedido` e o salva no banco. |
| `desativarCashbackUsado(Cliente cliente)` | Usado internamente pelo `concluirPedido` (e pelos agendamentos). Marca os cupons como `isActive=false` e os remove do saldo da `carteira` do cliente (se o saldo da carteira for esgotado). |
| `calcularSaldoAVencer(Cliente cliente)` | Usado pelo Agendador. Calcula e retorna a soma dos saldos de cupons **ativos** que vencem no dia seguinte (`hoje.plusDays(1)`). |
| `expirarCashback(Cliente cliente)` | Usado pelo Agendador. Desativa (`isActive=false`) os cupons cuja data de vencimento (`dataVencimento`) é anterior à data/hora atual, e deduz o valor expirado da `carteira` do cliente. |

<br> B. PedidoService.java

| Método | Ação do Cashback |
| :--- | :--- |
| `concluirPedido(...)` | **1.** Verifica o `usarCashbackIntegral` no DTO. **2.** Se `true`, chama `cashbackService.usarCashbackIntegral()` e usa o valor retornado para deduzir o total do pedido. **3.** Altera o status para `PAGO`. **4.** Chama `cashbackService.creditar()` (gerando o novo cupom) sobre o valor **líquido** final da compra. |

<br> C. TarefasAgendadas.java (`.utils`)

| Método | Função |
| :--- | :--- |
| `executarTarefasRecorrentes()` | Executado diariamente (ex: `0 0 0 * * ?`). Percorre todos os clientes, verifica cupons prestes a vencer e cupons vencidos, enviando notificações por e-mail. |

3. Endpoints Expostos (.controller)

| Verbo | URL | Função |
| :--- | :--- | :--- |
| **GET** | `/cashbacks/cliente/{id}` | **[Read]** Consulta todos os registros de Cashback ativos de um cliente. |
| **POST** | `/cashbacks/adicionar/{clienteId}` | **[Admin/Teste]** Cria um novo registro de Cashback com um valor específico (não é o fluxo de

<br><br>
## 2. 🔒 Segurança e Autenticação (JWT) (Hyago Guimarães)
A segurança da API é implementada utilizando o Spring Security para controle de acesso. O modelo de autenticação é baseado em JSON Web Tokens (JWT), garantindo que as requisições sejam stateless e seguras.<br>
* Autenticação: O cliente ou funcionário envia credenciais de login para o endpoint /login. Se as credenciais forem válidas (verificadas pelo UsuarioDetalheImpl), um token JWT é gerado utilizando o JwtUtil e retornado ao usuário.
* Autorização: Em requisições subsequentes, o token é validado pelo JwtAuthorizationFilter.
* Configuração: A classe ConfigSeguranca.java define as regras de autorização, liberando o acesso para endpoints públicos (como o Swagger e o Login) e exigindo o token para rotas protegidas.

<br><br>
## 3. ⭐ Wishlists - Lista de desejos (Natália Siqueira)
O módulo de Wishlist permite que clientes logados rastreiem produtos de interesse. É um recurso relacional que garante a unicidade dos dados e oferece operações de gerenciamento de lista.<br>
| Classe | Pacote | Função |
| :--- | :--- | :--- |
| **`WishlistItem`** | `.domain` | Entidade principal que armazena o relacionamento N:N entre `Cliente` e `Produto`. Possui uma restrição de unicidade para evitar que o mesmo produto seja adicionado duas vezes pelo mesmo cliente. |
| **`WishlistItemRepository`** | `.repository` | Define métodos de busca por ID do cliente e por combinação de `ClienteId` e `ProdutoId`, essenciais para a verificação de duplicidade e remoção. |
| **`WishlistResponseDTO`** | `.dto` | DTO de saída utilizado para listar os itens, retornando informações essenciais do produto e da categoria para o cliente de forma simplificada. |

<br><br>
## 4. 📝 Assinaturas de Produtos (Jessica Lima)
O módulo de Assinaturas permite que os clientes contratem planos recorrentes, implementando um modelo de negócio comum em serviços de entrega de café por recorrência.

<br> 1. Modelo de Entidades e Estados  
| Classe | Pacote | Função |
| :--- | :--- | :--- |
| **Plano** | `.domain` | Entidade que define as características do serviço recorrente (ex: Plano Mensal, Semestral). |
| **Assinatura** | `.domain` | Entidade que rastreia a inscrição do cliente em um `Plano` específico. |
| **StatusAssinatura** | `.domain` | **ENUM** que define os estados possíveis da assinatura (ex: ATIVA, CANCELADA, PENDENTE). |
| **AssinaturaDTO** | `.dto` | DTOs de leitura e escrita para comunicação com o cliente/API. |

<br> 2. Fluxo de Serviço (AssinaturaService.java)  
| Método | Função |
| :--- | :--- |
| **Criação (POST)** | Registra a inscrição do cliente em um `Plano`. Inicializa a assinatura com o `StatusAssinatura.PENDENTE` (ou o estado inicial definido). |
| **Atualização (PUT)** | Permite alterações no plano ou no status da assinatura (ex: mudar de ATIVA para CANCELADA). |
| **Notificação/Processamento** | Contém a lógica para processar renovações e verificar datas de cobrança (não detalhado, mas inferido pelo módulo). |

<br> 3. Endpoints Expostos (AssinaturaController.java)  
| Verbo | URL | Função |
| :--- | :--- | :--- |
| **GET** | `/assinaturas` | Lista todas as assinaturas do sistema ou de um cliente específico (se a URL for aninhada). |
| **GET** | `/assinaturas/{id}` | Busca uma assinatura específica pelo ID. |
| **POST** | `/assinaturas` | Cria uma nova assinatura (contratação de um plano). |
| **PUT** | `/assinaturas/{id}` | Atualiza os detalhes ou o status de uma assinatura. |
| **DELETE** | `/assinaturas/{id}` | Remove/Cancela logicamente a assinatura. |

# .
# 📂 Estrutura de Pacotes da API E-Commerce
```
📁 trabalho-final-API
└── 📁 src
    └── 📁 main
        ├── 📁 java
        │   └── 📁 org
        │       └── 📁 serratec
        │           └── 📁 trabalhoFinal
        │               ├── ☕ TrabalhoFinalApplication.java
        │               ├── 📁 config
        │               │   ├── ☕ ConfigSeguranca.java
        │               │   └── ☕ SwaggerConfig.java
        │               ├── 📁 controller
        │               │   ├── ☕ AssinaturaController.java
        │               │   ├── ☕ CashbackController.java
        │               │   ├── ☕ CategoriaController.java
        │               │   ├── ☕ ClienteController.java
        │               │   ├── ☕ EnderecoController.java
        │               │   ├── ☕ FuncionarioController.java
        │               │   ├── ☕ PedidoController.java
        │               │   ├── ☕ PlanoController.java
        │               │   ├── ☕ ProdutoController.java
        │               │   ├── ☕ RecomendacaoController.java
        │               │   └── ☕ WishlistController.java
        │               ├── 📁 domain
        │               │   ├── ☕ Assinatura.java
        │               │   ├── ☕ Cashback.java
        │               │   ├── ☕ Categoria.java
        │               │   ├── ☕ Cliente.java
        │               │   ├── ☕ Endereco.java
        │               │   ├── ☕ Funcionario.java
        │               │   ├── ☕ ItemPedido.java
        │               │   ├── ☕ Pedido.java
        │               │   ├── ☕ Plano.java
        │               │   ├── ☕ Produto.java
        │               │   ├── ☕ StatusAssinatura.java
        │               │   ├── ☕ StatusPedido.java
        │               │   ├── ☕ Usuario.java
        │               │   └── ☕ WishlistItem.java
        │               ├── 📁 dto
        │               │   ├── ☕ AssinaturaDTO.java
        │               │   ├── ☕ CarrinhoResponseDTO.java
        │               │   ├── ☕ CashbackDTO.java
        │               │   ├── ☕ CashbackResponseDTO.java
        │               │   ├── ☕ CategoriaDTO.java
        │               │   ├── ☕ ClienteAtualizacaoDTO.java
        │               │   ├── ☕ ClienteCriacaoDTO.java
        │               │   ├── ☕ ClienteDTO.java
        │               │   ├── ☕ EnderecoDTO.java
        │               │   ├── ☕ EstoqueDTO.java
        │               │   ├── ☕ FuncionarioDTO.java
        │               │   ├── ☕ ItemPedidoCriacaoDTO.java
        │               │   ├── ☕ ItemPedidoDTO.java
        │               │   ├── ☕ LoginDTO.java
        │               │   ├── ☕ PedidoCriacaoDTO.java
        │               │   ├── ☕ PedidoDTO.java
        │               │   ├── ☕ PedidoStatusAtualizacaoDTO.java
        │               │   ├── ☕ PlanoDTO.java
        │               │   ├── ☕ ProdutoDTO.java
        │               │   └── ☕ WishlistResponseDTO.java
        │               ├── 📁 exception
        │               │   ├── ☕ ControllerExceptionHandler.java
        │               │   ├── ☕ EmailException.java
        │               │   ├── ☕ EstoqueInsuficienteException.java
        │               │   ├── ☕ ExternalServiceException.java
        │               │   ├── ☕ NotFoundException.java
        │               │   ├── ☕ SaldoInsuficienteException.java
        │               │   └── ☕ SenhaException.java
        │               ├── 📁 repository
        │               │   ├── ☕ AssinaturaRepository.java
        │               │   ├── ☕ CashbackRepository.java
        │               │   ├── ☕ CategoriaRepository.java
        │               │   ├── ☕ ClienteRepository.java
        │               │   ├── ☕ EnderecoRepository.java
        │               │   ├── ☕ FuncionarioRepository.java
        │               │   ├── ☕ PedidoRepository.java
        │               │   ├── ☕ PlanoRepository.java
        │               │   ├── ☕ ProdutoRepository.java
        │               │   ├── ☕ UsuarioRepository.java
        │               │   └── ☕ WishlistItemRepository.java
        │               ├── 📁 security
        │               │   ├── ☕ JwtAuthenticationFilter.java
        │               │   ├── ☕ JwtAuthorizationFilter.java
        │               │   └── ☕ JwtUtil.java
        │               └── 📁 service
        │                   ├── ☕ AssinaturaService.java
        │                   ├── ☕ CashbackService.java
        │                   ├── ☕ CategoriaService.java
        │                   ├── ☕ ClienteService.java
        │                   ├── ☕ DescontoService.java
        │                   ├── ☕ EmailService.java
        │                   ├── ☕ EnderecoService.java
        │                   ├── ☕ FuncionarioService.java
        │                   ├── ☕ PedidoService.java
        │                   ├── ☕ PlanoService.java
        │                   ├── ☕ ProdutoService.java
        │                   ├── ☕ RecomendacaoService.java
        │                   ├── ☕ UsuarioDetalheImpl.java
        │                   └── ☕ WishlistService.java
        └── 📁 utils
            └── ☕ TarefasAgendadas.java

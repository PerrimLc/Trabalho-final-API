# API RESTful - Trabalho Final: SerraBucks
<span style="font-size: 2px;">Serratec - Residência TIC/ Software 2025.2 - Prof.: Alberto Paz.</span> 
<br><small>  Grupo 4: Gelverson Tiago, Giselle Garcia, Hyago Guimarães, Jessica Lima, Lucas Perrin e Natália Siqueira.</small>
<br>

<span> O **SerraBucks, e-Commerce de Cafés**, é uma API RESTful completa desenvolvida em Java com Spring Boot, projetada para gerenciar todo o ciclo de vida de vendas de uma loja de comércio varejista. A aplicação utiliza o padrão em camadas e implementa CRUDs robustos para entidades centrais como `Cliente`, `Produto` e `Categoria`. O sistema de pedidos (carrinho) é totalmente funcional, suportando relacionamentos N:N via `ItemPedido` e gerenciamento de estoque, além de consumir o serviço externo do ViaCEP para preenchimento automático de endereço do cliente. Como recurso de valor agregado, a API possui um sistema de **Cashback baseado em cupons** que permite ao cliente gerar crédito em todas as compras e utilizá-lo integralmente em pedidos futuros, com regras de vencimento controladas por tarefas agendadas. A segurança é gerenciada por Spring Security com JWT, e a API conta com tratamento de exceções customizado e documentação via Swagger/OpenAPI.</span>

# 🎯 Funcionalidades Extras (Partes Individuais)
### 1. Sistema de Cashback (Giselle Garcia)
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
* 

### 2. Fluxo de Serviço (.service)
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

### 3. Endpoints Expostos (.controller)

| Verbo | URL | Função |
| :--- | :--- | :--- |
| **GET** | `/cashbacks/cliente/{id}` | **[Read]** Consulta todos os registros de Cashback ativos de um cliente. |
| **POST** | `/cashbacks/adicionar/{clienteId}` | **[Admin/Teste]** Cria um novo registro de Cashback com um valor específico (não é o fluxo de

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
        │               │   ├── ☕ MailConfig.java
        │               │   └── ☕ SwaggerConfig.java
        │               ├── 📁 controller
        │               │   ├── ☕ AssinaturaController.java
        │               │   ├── ☕ CashbackController.java
        │               │   ├── ☕ CategoriaController.java
        │               │   ├── ☕ ClienteController.java
        │               │   ├── ☕ EnderecoController.java
        │               │   ├── ☕ EstoqueController.java
        │               │   ├── ☕ FuncionarioController.java
        │               │   ├── ☕ PedidoController.java
        │               │   ├── ☕ PlanoController.java
        │               │   ├── ☕ ProdutoController.java
        │               │   ├── ☕ UsuarioController.java
        │               │   └── ☕ WishlistController.java
        │               ├── 📁 domain
        │               │   ├── ☕ Assinatura.java
        │               │   ├── ☕ Cashback.java
        │               │   ├── ☕ Categoria.java
        │               │   ├── ☕ Cliente.java
        │               │   ├── ☕ Endereco.java
        │               │   ├── ☕ Estoque.java
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
        │               │   ├── ☕ UsuarioDTO.java
        │               │   └── ☕ WishlistResponseDTO.java
        │               ├── 📁 exception
        │               │   ├── ☕ ControllerExceptionHandler.java
        │               │   ├── ☕ EmailException.java
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
        │               │   ├── ☕ EstoqueRepository.java
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
        │                   ├── ☕ EstoqueService.java
        │                   ├── ☕ FuncionarioService.java
        │                   ├── ☕ PedidoService.java
        │                   ├── ☕ PlanoService.java
        │                   ├── ☕ ProdutoService.java
        │                   ├── ☕ UsuarioDetalheImpl.java
        │                   └── ☕ WishlistService.java
        └── 📁 utils
            └── ☕ TarefasAgendadas.java

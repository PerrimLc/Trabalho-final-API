package org.serratec.trabalhoFinal.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.serratec.trabalhoFinal.domain.Cashback;
import org.serratec.trabalhoFinal.domain.Cliente;
import org.serratec.trabalhoFinal.domain.ItemPedido;
import org.serratec.trabalhoFinal.domain.Pedido;
import org.serratec.trabalhoFinal.domain.Produto;
import org.serratec.trabalhoFinal.domain.StatusPedido;
import org.serratec.trabalhoFinal.dto.CarrinhoResponseDTO;
import org.serratec.trabalhoFinal.dto.ItemPedidoCriacaoDTO;
import org.serratec.trabalhoFinal.dto.ItemPedidoDTO;
import org.serratec.trabalhoFinal.dto.PedidoCriacaoDTO;
import org.serratec.trabalhoFinal.dto.PedidoDTO;
import org.serratec.trabalhoFinal.exception.NotFoundException;
import org.serratec.trabalhoFinal.repository.ClienteRepository;
import org.serratec.trabalhoFinal.repository.PedidoRepository;
import org.serratec.trabalhoFinal.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepo;
    private final ClienteRepository clienteRepo;
    private final ProdutoRepository produtoRepo;
    private final CashbackService cashbackService;
    private final EmailService emailService;
    private final ProdutoService produtoService;
    private final DescontoService descontoService;

    public PedidoService(PedidoRepository pedidoRepo, ClienteRepository clienteRepo, ProdutoRepository produtoRepo,
                         CashbackService cashbackService, EmailService emailService, ProdutoService produtoService,
                         DescontoService descontoService) {
        this.pedidoRepo = pedidoRepo;
        this.clienteRepo = clienteRepo;
        this.produtoRepo = produtoRepo;
        this.cashbackService = cashbackService;
        this.emailService = emailService;
        this.produtoService = produtoService;
        this.descontoService = descontoService;
    }

    @Transactional
    public PedidoDTO criar(PedidoCriacaoDTO dto) {
        Cliente cliente = clienteRepo.findById(dto.getClienteId())
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.CRIADO);

        boolean primeiroPedido = !pedidoRepo.existsByClienteId(cliente.getId());

        for (ItemPedidoCriacaoDTO itemDTO : dto.getItens()) {
            Produto produto = produtoRepo.findById(itemDTO.getProdutoId())
                    .orElseThrow(() -> new NotFoundException("Produto não encontrado: " + itemDTO.getProdutoId()));

            if (!produtoService.verificarEstoque(produto.getId(), itemDTO.getQuantidade())) {
                throw new RuntimeException("Estoque insuficiente para o produto: " + produto.getNome());
            }

            produtoService.darBaixaEstoque(produto.getId(), itemDTO.getQuantidade());

            ItemPedido item = new ItemPedido();
            item.setPedido(pedido);
            item.setProduto(produto);
            item.setQuantidade(itemDTO.getQuantidade());
            item.setValorVenda(produto.getPreco());

            // Aplica desconto apenas no primeiro pedido
            BigDecimal desconto = primeiroPedido ? descontoService.calcularDesconto(produto.getPreco()) : BigDecimal.ZERO;
            item.setDesconto(desconto.multiply(BigDecimal.valueOf(itemDTO.getQuantidade())));

            pedido.adicionarItem(item);
        }

        Pedido saved = pedidoRepo.save(pedido);
        return toDto(saved);
    }

    public PedidoDTO buscarPorId(Long id) {
        Pedido p = pedidoRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado"));
        return toDto(p);
    }

    public List<PedidoDTO> listarTodos() {
        return pedidoRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public void deletar(Long id) {
        if (!pedidoRepo.existsById(id)) {
            throw new NotFoundException("Não foi possível excluir o pedido! Ele não existe.");
        }
        pedidoRepo.deleteById(id);
    }

    @Transactional
    public CarrinhoResponseDTO adicionarProduto(Long clienteId, @Valid ItemPedidoCriacaoDTO dto) {
        Cliente cliente = clienteRepo.findById(clienteId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com o ID: " + clienteId));

        Pedido pedido;
        if (!pedidoRepo.existsByClienteIdAndStatus(clienteId, StatusPedido.PENDENTE)) {
            pedido = new Pedido();
            pedido.setCliente(cliente);
            pedido.setStatus(StatusPedido.PENDENTE);
        } else {
            pedido = pedidoRepo.findByClienteIdAndStatus(clienteId, StatusPedido.PENDENTE);
        }

        Produto produto = produtoRepo.findById(dto.getProdutoId())
                .orElseThrow(() -> new NotFoundException("Produto não encontrado"));

        if (!produtoService.verificarEstoque(produto.getId(), dto.getQuantidade())) {
            throw new RuntimeException("Estoque insuficiente para o produto: " + produto.getNome());
        }

        produtoService.darBaixaEstoque(produto.getId(), dto.getQuantidade());

        boolean primeiroPedido = !pedidoRepo.existsByClienteId(clienteId);
        BigDecimal descontoUnitario = primeiroPedido ? descontoService.calcularDesconto(produto.getPreco()) : BigDecimal.ZERO;

        ItemPedido item = new ItemPedido(produto, pedido, dto.getQuantidade());
        item.setDesconto(descontoUnitario.multiply(BigDecimal.valueOf(dto.getQuantidade())));
        item.setValorVenda(produto.getPreco());

        pedido.adicionarItem(item);

        PedidoDTO pedidoDto = toDto(pedidoRepo.save(pedido));
        return new CarrinhoResponseDTO(pedidoDto.getItens(), pedido.getTotal());
    }

    @Transactional
    public PedidoDTO concluirPedido(Long pedidoId, boolean usarCashback) {
        Pedido pedido = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado."));

        if (!pedido.getStatus().equals(StatusPedido.PENDENTE)) {
            throw new IllegalArgumentException(
                    "Não é possível concluir o pedido. O status atual é " + pedido.getStatus().name() + ".");
        }

        BigDecimal totalDoPedido = pedido.getTotal();

        if (usarCashback) {
            BigDecimal saldoCashback = pedido.getCliente().getCarteira();
            if (saldoCashback.compareTo(totalDoPedido) >= 0) {
                saldoCashback = saldoCashback.subtract(totalDoPedido);
                totalDoPedido = BigDecimal.ZERO;
                pedido.getCliente().setCarteira(saldoCashback);
            } else {
                totalDoPedido = totalDoPedido.subtract(saldoCashback);
                pedido.getCliente().setCarteira(BigDecimal.ZERO);
            }
        }

        Cashback cashback = cashbackService.ganharCashback(pedido.getCliente(), totalDoPedido);
        pedido.getCliente().aumentarCarteira(cashback);

        pedido.setStatus(StatusPedido.PAGO);

        emailService.enviarNotificacaoCashback(pedido, cashback.getSaldo(), pedido.getCliente().getCarteira(),
                totalDoPedido, pedido.getTotal());

        PedidoDTO dto = toDto(pedidoRepo.save(pedido));
        dto.setTotal(totalDoPedido);
        cashbackService.desativarCashbackUsado(pedido.getCliente());

        return dto;
    }

    private PedidoDTO toDto(Pedido p) {
        PedidoDTO dto = new PedidoDTO();
        dto.setId(p.getId());
        dto.setClienteNome(p.getCliente().getNome());
        dto.setStatus(p.getStatus().name());
        dto.setDataCriacao(p.getDataCriacao().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        List<ItemPedidoDTO> itensDto = p.getItens().stream().map(item -> {
            ItemPedidoDTO idto = new ItemPedidoDTO();
            idto.setProdutoNome(item.getProduto().getNome());
            idto.setQuantidade(item.getQuantidade());
            idto.setValorUnitario(item.getValorVenda());
            idto.setDesconto(item.getDesconto() != null ? item.getDesconto() : BigDecimal.ZERO);
            idto.setSubtotal(item.getValorVenda()
                    .multiply(BigDecimal.valueOf(item.getQuantidade()))
                    .subtract(item.getDesconto() != null ? item.getDesconto() : BigDecimal.ZERO));
            return idto;
        }).collect(Collectors.toList());

        dto.setItens(itensDto);
        dto.setTotal(p.getTotal());
        dto.setValorDescontoTotal(p.getValorDescontoTotal());

        return dto;
    }
}
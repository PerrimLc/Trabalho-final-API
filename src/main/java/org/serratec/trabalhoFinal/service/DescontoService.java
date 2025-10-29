package org.serratec.trabalhoFinal.service;

import org.serratec.trabalhoFinal.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DescontoService {

    private final PedidoRepository pedidoRepository;
    private static final BigDecimal DESCONTO_PERCENTUAL = new BigDecimal("0.10"); // 10%

    public DescontoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    public BigDecimal aplicarDesconto(BigDecimal valorTotalInicial, Long clienteId) {
        boolean clienteJaFezPedido = pedidoRepository.existsByClienteId(clienteId);

        if (!clienteJaFezPedido) {
            BigDecimal valorDesconto = valorTotalInicial.multiply(DESCONTO_PERCENTUAL)
                    .setScale(2, RoundingMode.HALF_UP);
            return valorTotalInicial.subtract(valorDesconto);
        } else {
            return valorTotalInicial;
        }
    }

    public BigDecimal calcularDesconto(BigDecimal preco) {
        BigDecimal desconto = preco.multiply(DESCONTO_PERCENTUAL)
                .setScale(2, RoundingMode.HALF_UP);
        return desconto;
    }
}

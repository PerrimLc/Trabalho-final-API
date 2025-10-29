package org.serratec.trabalhoFinal.service;

import java.math.BigDecimal;

import org.serratec.trabalhoFinal.domain.Cliente;
import org.serratec.trabalhoFinal.domain.Funcionario;
import org.serratec.trabalhoFinal.domain.Pedido;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

	private final JavaMailSender mailSender;

	public EmailService(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}
	
	public void enviarNotificacaoCliente(Cliente cliente, String acao) {
		try {
			SimpleMailMessage msg = new SimpleMailMessage();
			msg.setTo(cliente.getEmail());
			msg.setSubject(acao);
			msg.setText("Olá " + cliente.getNome() + ", seu cadastro foi concluido com sucesso. Aproveite tudo que há de melhor em nossa cafeteria!");
			mailSender.send(msg);
			
			System.out.println("E-mail enviado para " + cliente.getEmail());
			
		} catch (Exception e) {
			System.err.println("Falha ao enviar e-mail: " + e.getMessage());
		}
	}
	
	public void enviarNotificacaoFuncionario(Funcionario funcionario, String acao) {
		try {
			SimpleMailMessage msg = new SimpleMailMessage();
			msg.setTo(funcionario.getEmail());
			msg.setSubject(acao);
			msg.setText("Olá " + funcionario.getNome() + ", seu cadastro foi concluido com sucesso. Seja bem-vindo a nossa equipe!");
			mailSender.send(msg);
			
			System.out.println("E-mail enviado para " + funcionario.getEmail());
			
		} catch (Exception e) {
			System.err.println("Falha ao enviar e-mail: " + e.getMessage());
		}
	}
	
	public void enviarNotificacaoCashback(Pedido pedido, BigDecimal valorCreditado, BigDecimal novoSaldo, BigDecimal totalDesconto, BigDecimal totalDoPedido) {
        Cliente cliente = pedido.getCliente();
        
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(cliente.getEmail());
            msg.setSubject("Cashback Creditado com Sucesso! Pedido #" + pedido.getId());
            
            String corpoEmail = String.format(
                "Olá %s,\n\n" +
                "Parabéns! Seu pedido #%d foi concluído.\n" +
                "Seu pedido foi de R$ %.2f, mas com o seu cashback, você pagou um total de R$ %.2f.\n" +
                "Você acaba de ganhar R$ %.2f de Cashback!\n\n" +
                "Seu novo saldo total de Cashback é de R$ %.2f.\n\n" +
                "Use seu saldo na próxima compra!\n\n" +
                "Atenciosamente,\n" +
                "SerraBucks - E-Commerce de Cafés",
                cliente.getNome(), 
                pedido.getId(), 
                totalDoPedido,
                totalDesconto,
                valorCreditado, 
                novoSaldo
            );
            
            msg.setText(corpoEmail);
            mailSender.send(msg);
            System.out.println("E-mail de Cashback enviado para " + cliente.getEmail());
        } catch (Exception e) {
            System.err.println("Falha ao enviar e-mail de Cashback: " + e.getMessage());
        }
    }

	public void cashbackAVencer(Cliente c, BigDecimal saldoAVencer) {
		
		try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(c.getEmail());
            msg.setSubject("Seu cashback está vencendo!");
            
            String corpoEmail = String.format(
                "Olá %s,\n\n" +
                "Você tem um saldo de cashback de R$ %.2f que vence amanhã.\n\n" +
                "Corra para o nosso site, aproveite as novidades e não perca seu beneficio!\n\n" +
               
                "Atenciosamente,\n" +
                "SerraBucks - E-Commerce de Cafés",
                c.getNome(), 
                saldoAVencer
            );
            
            msg.setText(corpoEmail);
            mailSender.send(msg);
            System.out.println("E-mail de Cashback enviado para " + c.getEmail());
        } catch (Exception e) {
            System.err.println("Falha ao enviar e-mail de Cashback: " + e.getMessage());
        }
		
	}

	public void notificarCashbackVencido(Cliente c, BigDecimal cashbackVencido) {
		
		try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(c.getEmail());
            msg.setSubject("Seu cashback venceu...");
            
            String corpoEmail = String.format(
                "Olá %s,\n\n" +
                "Seu saldo de cashback de R$ %.2f venceu :( \n\n" +
                "Mas não desanime, você ainda tem R$ %.2f de saldo para gastar!\n\n" +
                "Confira nossos produtos disponíveis.\n\n" +
                "Atenciosamente,\n" +
                "SerraBucks - E-Commerce de Cafés",
                c.getNome(), 
                cashbackVencido,
                c.getCarteira()
            );
            
            msg.setText(corpoEmail);
            mailSender.send(msg);
            System.out.println("E-mail de Cashback enviado para " + c.getEmail());
        } catch (Exception e) {
            System.err.println("Falha ao enviar e-mail de Cashback: " + e.getMessage());
        }
		
	}
}



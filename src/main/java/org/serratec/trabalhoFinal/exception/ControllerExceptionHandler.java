package org.serratec.trabalhoFinal.exception;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatusCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(EmailException.class)
    public ResponseEntity<ErrorResponse> handleEmail(EmailException ex, HttpServletRequest request) {
        System.out.println("EmailException: " + ex.getMessage());
        ErrorResponse body = buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro no Serviço de E-mail", ex.getMessage(), request.getRequestURI(), null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
	
	@ExceptionHandler(SenhaException.class)
    public ResponseEntity<ErrorResponse> handleSenha(SenhaException ex, HttpServletRequest request) {
        System.out.println("SenhaException: " + ex.getMessage());
        ErrorResponse body = buildError(HttpStatus.BAD_REQUEST, "Erro de Segurança", ex.getMessage(), request.getRequestURI(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
	
	@ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        System.out.println("DataIntegrityViolationException: " + ex.getMessage());
        ErrorResponse body = buildError(
                HttpStatus.CONFLICT,
                "Conflito de Dados",
                "O registro não pôde ser salvo pois já existe este dado no sistema.",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
	
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        System.out.println("NotFoundException: " + ex.getMessage());
        ErrorResponse body = buildError(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), request.getRequestURI(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternal(ExternalServiceException ex, HttpServletRequest request) {
        System.out.println("ExternalServiceException: " + ex.getMessage());
        ErrorResponse body = buildError(HttpStatus.BAD_GATEWAY, "Erro em serviço externo", ex.getMessage(), request.getRequestURI(), null);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegal(IllegalArgumentException ex, HttpServletRequest request) {
        System.out.println("IllegalArgumentException: " + ex.getMessage());
        ErrorResponse body = buildError(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage(), request.getRequestURI(), null);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, HttpServletRequest request) {
        System.out.println("ERRO INTERNO no endpoint: " + request.getRequestURI());
        ex.printStackTrace(); 

        ErrorResponse body = buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno",
                "Ocorreu um erro interno no servidor. Não foi possível concluir essa requisição!",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<ErrorResponse> handleSaldoInsuficiente(SaldoInsuficienteException ex, HttpServletRequest request) {
        ErrorResponse body = buildError(HttpStatus.BAD_REQUEST, "Operação Inválida", ex.getMessage(), request.getRequestURI(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
    
   
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatus status,
            HttpStatusCode status1,
            WebRequest request) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        String message = "Erros de validação nos campos";
        String path = request.getDescription(false).replace("uri=", "");
        ErrorResponse body = buildError(HttpStatus.BAD_REQUEST, message, "Verifique os campos inválidos", path, errors);

        System.out.println("Erro de validação: " + errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(body);
    }

    private String formatFieldError(FieldError e) {
        return e.getField() + ": " + Optional.ofNullable(e.getDefaultMessage()).orElse("inválido");
    }

    private ErrorResponse buildError(HttpStatus status, String error, String message, String path, @Nullable List<String> details) {
        ErrorResponse resp = new ErrorResponse();
        resp.setTimestamp(OffsetDateTime.now());
        resp.setStatus(status.value());
        resp.setError(error);
        resp.setMessage(message);
        resp.setPath(path);
        resp.setDetails(details == null ? Collections.emptyList() : details);
        return resp;
    }

    public static class ErrorResponse {
        private OffsetDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private List<String> details = new ArrayList<>();
		public OffsetDateTime getTimestamp() {
			return timestamp;
		}
		
		public void setTimestamp(OffsetDateTime timestamp) {
			this.timestamp = timestamp;
		}
		
		public int getStatus() {
			return status;
		}
		
		public void setStatus(int status) {
			this.status = status;
		}
		
		public String getError() {
			return error;
		}
		
		public void setError(String error) {
			this.error = error;
		}
		
		public String getMessage() {
			return message;
		}
		
		public void setMessage(String message) {
			this.message = message;
		}
		
		public String getPath() {
			return path;
		}
		
		public void setPath(String path) {
			this.path = path;
		}
		
		public List<String> getDetails() {
			return details;
		}
		
		public void setDetails(List<String> details) {
			this.details = details;
		}
    }
}
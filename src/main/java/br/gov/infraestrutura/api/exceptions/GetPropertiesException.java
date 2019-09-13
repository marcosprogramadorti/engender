package br.gov.infraestrutura.api.exceptions;

import java.io.IOException;

public class GetPropertiesException  extends IOException  /* RuntimeException */{
    /**
     * importante caso a exceção seja serializada
     */
    private static final long serialVersionUID = 1149241039409861914L;

    // constrói um objeto NumeroNegativoException com a mensagem passada por parâmetro
    public GetPropertiesException(String msg){
        super(msg);
    }

    // contrói um objeto NumeroNegativoException com mensagem e a causa dessa exceção, utilizado para encadear exceptions
    public GetPropertiesException(String msg, Throwable cause){
        super(msg, cause);
    }
}

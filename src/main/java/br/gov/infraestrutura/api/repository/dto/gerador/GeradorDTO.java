package br.gov.infraestrutura.api.repository.dto.gerador;

import java.util.List;

public class GeradorDTO {
	
	private String autor;
	private String nmModel;
	//Essa propriedade define quais os arquivos devem ser gerados.  
	private List<String> templateNames;
	
	
	public GeradorDTO() {
		super();
	}
	
	public String getAutor() {
		return autor;
	}
	public void setAutor(String autor) {
		this.autor = autor;
	}
	public String getNmModel() {
		return nmModel;
	}
	public void setNmModel(String nmModel) {
		this.nmModel = nmModel;
	}
	
	public List<String> getTemplateNames() {
		return templateNames;
	}
	public void setTemplateNames(List<String> templateNames) {
		this.templateNames = templateNames;
	}
	
	
	
	
	
}

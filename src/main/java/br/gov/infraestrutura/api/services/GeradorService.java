package br.gov.infraestrutura.api.services;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import br.gov.infraestrutura.api.repository.dto.gerador.GeradorDTO;

/**
 * 
 * Gerador de código fonte Java automático: Cria API Java customizada aos
 * padrões de projeto do Ministério automaticamente a partir do banco de dados.
 * Motivação: Garante aderência a arquitetura Codificação Genérica e Agilidade
 * em novas entregas
 * 
 * 
 * @author marcos.tavares
 */

public class GeradorService {

	private static final String SRC_MAIN_RESOURCES_GERADOR_CONFIG_PROPERTIES = "/src/main/resources/gerador/config.properties";
	public Properties prop = new Properties();

	public GeradorService(String fileConfigProperties) {
		super();
		getProperties(fileConfigProperties);

	}

	private void getProperties(String fileConfigProperties) {

		try {
			String pathRootApi;
			pathRootApi = new File(".").getCanonicalPath();
			String pathTemplate = pathRootApi + fileConfigProperties;
			FileInputStream ip;
			ip = new FileInputStream(pathTemplate);
			prop.load(ip);

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	public GeradorService() {
		super();
		getProperties(SRC_MAIN_RESOURCES_GERADOR_CONFIG_PROPERTIES);
	}

	public Map<String, String> doFilesOfTemplates(GeradorDTO geradorDTO) throws IOException {
		Map<String, String> resultado = new HashMap<String, String>();
		StringBuilder arqNovo = new StringBuilder();
		StringBuilder arqDtoNovo = new StringBuilder();
		FileInputStream is = null;
		String nameDTO = geradorDTO.getNmModel() + prop.getProperty("COMPLEMENT_OF_NAME_DTO");
		String pathDestinyFile = null;
		List<String> arquivosGerados = new ArrayList<String>();
		int total = 0;

		try {
			if (geradorDTO.getTemplateNames() != null && geradorDTO.getTemplateNames().size() > 0) {
				if (geradorDTO.getTemplateNames().get(0).equalsIgnoreCase("a")
						|| geradorDTO.getTemplateNames().get(0).equalsIgnoreCase("all")) {
					addAllTemplatesNames(geradorDTO);
				}
				for (String f : geradorDTO.getTemplateNames()) {
					String pathRootApi = new File(".").getCanonicalPath();
					String pathTemplate = pathRootApi + prop.getProperty("PATH_RESOURCES_GENERATOR") + f;
					arqNovo = recuperaArqTemplate(pathTemplate);
					if (arqNovo.indexOf(prop.getProperty("REPLACEMENT_KEY_REPOSITORY_NAME")) != -1) {
						arqNovo = ruleReplacement(geradorDTO.getNmModel(), arqNovo,
								prop.getProperty("REPLACEMENT_KEY_REPOSITORY_NAME"));
					}
					if (arqNovo.indexOf(prop.getProperty("REPLACEMENT_KEY_REPOSITORY_NAME_LOWERCASE")) != -1) {
						arqNovo = ruleReplacement(lowerCaseFirtLetter(geradorDTO.getNmModel()), arqNovo,
								prop.getProperty("REPLACEMENT_KEY_REPOSITORY_NAME_LOWERCASE"));
					}
					if (arqNovo.indexOf(prop.getProperty("REPLACEMENT_KEY_AUTOR")) != -1) {
						if (geradorDTO.getAutor() != null) {
							arqNovo = ruleReplacement(geradorDTO.getAutor(), arqNovo,
									prop.getProperty("REPLACEMENT_KEY_AUTOR"));
						} else {
							arqNovo = ruleReplacement("Gerador AutomÃ¡tico", arqNovo,
									prop.getProperty("REPLACEMENT_KEY_AUTOR"));
						}

					}

					String complementOfName = getNameByNameTemplate(f);
					String nomeDoArquivo = geradorDTO.getNmModel() + complementOfName
							+ prop.getProperty("FILE_EXTENSION_JAVA");

					// regra para pegar path/package customizado.
					if (arqNovo.indexOf("gerador.path.destiny.file") != -1) {
						pathDestinyFile = getPathFromTemplate(arqNovo);
						geraNovoArquivo(arqNovo, pathRootApi + pathDestinyFile + nomeDoArquivo);
					} else {
						// aqui os instalados (de caixa)
						if (complementOfName.equals(prop.getProperty("COMPLEMENT_OF_NAME_REPOSITORY"))) {
							geraNovoArquivo(arqNovo,
									pathRootApi + prop.getProperty("PATH_REPOSITORY_DESTINY_FILE") + nomeDoArquivo);
						}
						if (complementOfName.equals(prop.getProperty("COMPLEMENT_OF_NAME_SEVICE"))) {
							String pathModel = pathRootApi + prop.getProperty("PATH_ORIGIN_MODEL")
									+ geradorDTO.getNmModel() + prop.getProperty("FILE_EXTENSION_JAVA");
							arqDtoNovo = dtoFromEntity(pathModel, nameDTO);
							gerarArquivoDTO(nameDTO, arqDtoNovo, pathRootApi);
							total += arqDtoNovo.length();
							geraNovoArquivo(arqNovo,
									pathRootApi + prop.getProperty("PATH_SERVICE_DESTINY_FILE") + nomeDoArquivo);
						}
						if (complementOfName.equals(prop.getProperty("COMPLEMENT_OF_NAME_RESOURCE"))) {
							geraNovoArquivo(arqNovo,
									pathRootApi + prop.getProperty("PATH_RESOUCE_DESTINY_FILE") + nomeDoArquivo);
						}
					}

					arquivosGerados.add(nomeDoArquivo);
					total += arqNovo.length();

				}
			}

			resultado.put("statusProcessamento", "OK");
			resultado.put("totalCaracteresGerados", total + "");
			resultado.put("arquivosGerados", arquivosGerados.toString());

		} finally {
			if (is != null) {
				is.close();

			}
		}

		return resultado;
	}

	private void addAllTemplatesNames(GeradorDTO geradorDTO) {
		geradorDTO.getTemplateNames().remove(0);// remove all or a
		geradorDTO.getTemplateNames().add(prop.getProperty("REPOSITORY_TEMPLATE"));
		geradorDTO.getTemplateNames().add(prop.getProperty("SERVICE_TEMPLATE"));
		geradorDTO.getTemplateNames().add(prop.getProperty("RESOURCE_TEMPLATE"));
	}

	private String getPathFromTemplate(StringBuilder arqNovo) {
		if (arqNovo.indexOf("gerador.path.destiny.file") != -1) {
			int beginningOfTheLine = arqNovo.indexOf("gerador.path.destiny.file");
			int endOfTheLine = arqNovo.indexOf(";");
			String line = arqNovo.substring(beginningOfTheLine, endOfTheLine);
			arqNovo.delete(beginningOfTheLine, endOfTheLine);
			return line.substring(line.indexOf("="), endOfTheLine);
		}
		return null;
	}

	public Map<String, String> geraRepositoryFile(GeradorDTO geradorDTO) throws IOException {

		Map<String, String> resultado = new HashMap<String, String>();
		StringBuilder arqNovo = new StringBuilder();

		String pathRootApi = new File(".").getCanonicalPath();
		String pathTemplate = pathRootApi + prop.getProperty("PATH_RESOURCES_GENERATOR")
				+ prop.getProperty("REPOSITORY_TEMPLATE");

		arqNovo = recuperaArqTemplate(pathTemplate);

		arqNovo = ruleReplacement(geradorDTO.getNmModel(), arqNovo,
				prop.getProperty("REPLACEMENT_KEY_REPOSITORY_NAME"));

		String nomeDoArquivo = geradorDTO.getNmModel() + prop.getProperty("COMPLEMENT_OF_NAME_REPOSITORY")
				+ prop.getProperty("FILE_EXTENSION_JAVA");

		geraNovoArquivo(arqNovo, pathRootApi + prop.getProperty("PATH_REPOSITORY_DESTINY_FILE") + nomeDoArquivo);

		resultado.put("statusProcessamento", "OK");
		resultado.put("arquivoGerado", nomeDoArquivo);
		resultado.put("package", prop.getProperty("PATH_REPOSITORY_DESTINY_FILE"));

		return resultado;

	}

	public Map<String, String> geraServiceFile(GeradorDTO geradorDTO) throws IOException {

		Map<String, String> resultado = new HashMap<String, String>();
		StringBuilder arqServiceNovo = new StringBuilder();
		StringBuilder arqDtoNovo = new StringBuilder();

		String nameDTO = geradorDTO.getNmModel() + prop.getProperty("COMPLEMENT_OF_NAME_DTO");

		String pathRootApi = new File(".").getCanonicalPath();
		String pathTemplate = pathRootApi + prop.getProperty("PATH_RESOURCES_GENERATOR")
				+ prop.getProperty("SERVICE_TEMPLATE");

		String pathModel = pathRootApi + prop.getProperty("PATH_ORIGIN_MODEL") + geradorDTO.getNmModel()
				+ prop.getProperty("FILE_EXTENSION_JAVA");
		arqDtoNovo = dtoFromEntity(pathModel, nameDTO);

		arqServiceNovo = recuperaArqTemplate(pathTemplate);

		arqServiceNovo = ruleReplacement(geradorDTO.getNmModel(), arqServiceNovo,
				prop.getProperty("REPLACEMENT_KEY_REPOSITORY_NAME"));
		arqServiceNovo = ruleReplacement(lowerCaseFirtLetter(geradorDTO.getNmModel()), arqServiceNovo,
				prop.getProperty("REPLACEMENT_KEY_REPOSITORY_NAME_LOWERCASE"));

		String nomeDoArquivo = geradorDTO.getNmModel() + prop.getProperty("COMPLEMENT_OF_NAME_SEVICE")
				+ prop.getProperty("FILE_EXTENSION_JAVA");

		String pathDestiny = pathRootApi + prop.getProperty("PATH_SERVICE_DESTINY_FILE") + nomeDoArquivo;
		gerarArquivoDTO(nameDTO, arqDtoNovo, pathRootApi);
		gerarArquvioService(arqServiceNovo, pathDestiny);

		resultado.put("statusProcessamento", "OK");
		resultado.put("arquivoGerado", nomeDoArquivo);
		resultado.put("package", prop.getProperty("PATH_SERVICE_DESTINY_FILE"));

		return resultado;

	}

	public Map<String, String> geraResourceFile(GeradorDTO geradorDTO) throws IOException {

		Map<String, String> resultado = new HashMap<String, String>();
		StringBuilder arqNovo = new StringBuilder();

		String pathRootApi = new File(".").getCanonicalPath();
		String pathTemplate = pathRootApi + prop.getProperty("PATH_RESOURCES_GENERATOR")
				+ prop.getProperty("RESOURCE_TEMPLATE");

		arqNovo = recuperaArqTemplate(pathTemplate);

		arqNovo = ruleReplacement(geradorDTO.getNmModel(), arqNovo,
				prop.getProperty("REPLACEMENT_KEY_REPOSITORY_NAME"));
		arqNovo = ruleReplacement(lowerCaseFirtLetter(geradorDTO.getNmModel()), arqNovo,
				prop.getProperty("REPLACEMENT_KEY_REPOSITORY_NAME_LOWERCASE"));

		String nomeDoArquivo = geradorDTO.getNmModel() + prop.getProperty("COMPLEMENT_OF_NAME_RESOURCE")
				+ prop.getProperty("FILE_EXTENSION_JAVA");

		String pathDestiny = pathRootApi + prop.getProperty("PATH_RESOUCE_DESTINY_FILE") + nomeDoArquivo;
		geraNovoArquivo(arqNovo, pathDestiny);
		gerarArquvioService(arqNovo, pathDestiny);

		resultado.put("statusProcessamento", "OK");
		resultado.put("arquivoGerado", nomeDoArquivo);
		resultado.put("package", prop.getProperty("PATH_RESOUCE_DESTINY_FILE"));

		return resultado;

	}

	public Map<String, String> geraFilesCRUD(GeradorDTO geradorDTO) throws IOException {

		Map<String, String> resultado = new HashMap<String, String>();

		geraRepositoryFile(geradorDTO);
		geraServiceFile(geradorDTO);
		geraResourceFile(geradorDTO);

		resultado.put("statusProcessamento", "OK");

		return resultado;

	}

	public Map<String, String> geraFilesCRUDList(List<GeradorDTO> lista) throws IOException {

		Map<String, String> resultado = new HashMap<String, String>();

		for (GeradorDTO g : lista) {

			geraRepositoryFile(g);
			geraServiceFile(g);
			geraResourceFile(g);

		}

		resultado.put("statusProcessamento", "OK");

		return resultado;

	}

	public Map<String, String> doListFilesOfTemplates(List<GeradorDTO> lista) throws IOException {

		Map<String, String> resultado = new HashMap<String, String>();
		List<Map<String, String>> filesResult = new ArrayList<>();

		for (GeradorDTO g : lista) {

			filesResult.add(doFilesOfTemplates(g));

		}

		resultado.put("statusProcessamento", filesResult.toString());

		return resultado;

	}

	private void gerarArquvioService(StringBuilder arqNovo, String pathDestiny) throws IOException {
		geraNovoArquivo(arqNovo, pathDestiny);
	}

	private void gerarArquivoDTO(String nmModel, StringBuilder arqDtoNovo, String pathRootApi) throws IOException {
		if (arqDtoNovo != null) {
			String nomeDoArqDTO = nmModel + prop.getProperty("FILE_EXTENSION_JAVA");
			geraNovoArquivo(arqDtoNovo, pathRootApi + prop.getProperty("PATH_DTO_DESTINY_FILE") + nomeDoArqDTO);
		}
	}

	private void instanciaDinamica() {
		Class<?> clazz = null;
		try {
			clazz = Class.forName("br.gov.infraestrutura.api.model.Autor");
			Constructor<?> ctor = clazz.getConstructor();
			Object object = ctor.newInstance();
			String[] lista = getProperties(object);

		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {

		}
	}

	private StringBuilder recuperaArqTemplate(String pathTemplate) throws FileNotFoundException, IOException {
		StringBuilder arq = new StringBuilder();
		FileInputStream is;
		is = new FileInputStream(pathTemplate);
		try {
			int content;

			while ((content = is.read()) != -1) {
				// convert to char and display it
				arq.append((char) content);
			}
		} finally {
			is.close();
		}
		return arq;
	}

	private StringBuilder dtoFromEntity(String pathTemplate, String nome) throws IOException {

		StringBuilder arq = new StringBuilder();
		FileInputStream is;
		List<String> typesDTO = new ArrayList<String>();
		is = new FileInputStream(pathTemplate);
		try {
			int content;
			StringBuilder linha = new StringBuilder();
			while ((content = is.read()) != -1) {
				linha.append((char) content);
				if (content == 10) {
					if (validaLinha(linha)) {
						if (linha.indexOf("private") != -1) {
							String typeDTO = changeToDtoType(linha);
							if (typeDTO != null) {
								typesDTO.add(typeDTO);
							}
						}
						arq.append(linha);
					}
					linha.setLength(0);
				}

			}

		} finally {
			is.close();
		}

		arq.insert(0, prop.getProperty("PACKAGE_DTO") + (char) 10);
		insertDTOname(nome, arq);
		arq = updateForDTO(arq, typesDTO);
		return arq;
	}

	private StringBuilder updateForDTO(StringBuilder arq, List<String> typesDTO) {
		for (String t : typesDTO) {
			arq = replaceAll(arq, t, t + prop.getProperty("COMPLEMENT_OF_NAME_DTO"));
		}
		return arq;
	}

	private void insertDTOname(String nome, StringBuilder arq) {
		int i = arq.indexOf("public class");
		int f = arq.indexOf("implements");
		arq.replace(i + 13, f - 1, nome);
	}

	private boolean validaLinha(StringBuilder linha) {
		if (linha.indexOf("import ") != -1) {
			if (linha.indexOf("import javax.persistence") != -1) {
				return false;
			}

			return true;
		}
		if (linha.indexOf("private") != -1) {
			return true;
		}
		if (linha.indexOf("public") != -1) {
			return true;
		}
		if (linha.indexOf("return") != -1) {
			return true;
		}
		if (linha.indexOf("this.") != -1) {
			return true;
		}
		if (linha.indexOf("@NotBlank") != -1) {
			return true;
		}
		if (linha.indexOf("@Size") != -1) {
			return true;
		}
		if (linha.indexOf("@Past") != -1) {
			return true;
		}
		if (linha.indexOf("@DateTimeFormat") != -1) {
			return true;
		}
		if (linha.indexOf("@NotEmpty") != -1) {
			return true;
		}
		if (linha.indexOf("@NotNull") != -1) {
			return true;
		}
		if (linha.indexOf("@NumberFormat") != -1) {
			return true;
		}
		if (linha.indexOf("@Pattern") != -1) {
			return true;
		}
		if (linha.indexOf("}") != -1) {
			return true;
		}

		return false;
	}

	private String changeToDtoType(StringBuilder line) {
		int i = line.indexOf("private ");
		int f = line.indexOf(" ", i + 8);
		String newType = line.substring(i + 8, f);
		// ex.: List<
		if (newType.indexOf("<") != -1) {
			newType = newType.substring(newType.indexOf("<"), newType.indexOf(">"));
			i += newType.indexOf("<");
			f -= 1;
		}
		if (!checkType.isInternalType(newType)) {
			return newType;
		}
		return null;
	}

	private void geraNovoArquivo(StringBuilder arqNovo, String path) throws IOException {
		FileWriter fw = null;
		BufferedWriter bw = null;
		boolean createFile = false;
		try {
			File arquivo = new File(path);
			// verifica se o arquivo ou diretÃ³rio existe
			boolean existe = arquivo.exists();
			if (!existe) {
				// cria um arquivo (vazio)
				createFile = arquivo.createNewFile();
				if (createFile) {
					// cria um diretÃ³rio
					arquivo.mkdir();

					// construtor que recebe tambÃ©m como argumento se o conteÃºdo serÃ¡
					// acrescentado
					// ao invÃ©s de ser substituÃ­do (append)
					fw = new FileWriter(arquivo, true);
					// construtor recebe como argumento o objeto do tipo FileWriter
					bw = new BufferedWriter(fw);
					bw.write(arqNovo.toString());
					arquivo = null;

				}

			}

		} finally {
			if (bw != null) {
				bw.close();
			}
			if (fw != null) {
				fw.close();
			}

		}
	}

	private StringBuilder ruleReplacement(String newValue, StringBuilder arqNovo, String oldValue) {
		arqNovo = replaceAll(arqNovo, oldValue, newValue);
		return arqNovo;
	}

	public static StringBuilder replaceAll(StringBuilder sb, String find, String replace) {
		return new StringBuilder(Pattern.compile(find).matcher(sb).replaceAll(replace));
	}

	public <T> String[] getProperties(T entity) {
		String[] properties = null;
		try {
			BeanInfo entityInfo = Introspector.getBeanInfo(entity.getClass());
			PropertyDescriptor[] propertyDescriptors = entityInfo.getPropertyDescriptors();
			properties = new String[propertyDescriptors.length];
			for (int i = 0; i < propertyDescriptors.length; i++) {
				Object propertyValue = propertyDescriptors[i].getReadMethod().invoke(entity);
				if (propertyValue != null) {
					properties[i] = propertyValue.toString();
				} else {
					properties[i] = null;
				}
			}
		} catch (Exception e) {
			// Handle your exception here.
		}
		return properties;
	}

	public String lowerCaseFirtLetter(String valor) {
		return valor.substring(0, 1).toLowerCase() + valor.substring(1);
	}

	public String getNameByNameTemplate(String valor) {
		valor = valor.substring(0, valor.indexOf("."));
		valor = valor.substring(0, 1).toUpperCase() + valor.substring(1);
		return valor;
	}

}

class checkType {

	private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();
	private static final Set<String> INTERNAL_TYPE = getInternalType();
	private static final Set<String> TYPE_LIST = getTypeList();

	public static boolean isWrapperType(Class<?> clazz) {
		return WRAPPER_TYPES.contains(clazz);
	}

	public static boolean isInternalType(String value) {
		return INTERNAL_TYPE.contains(value.toLowerCase());
	}

	private static Set<Class<?>> getWrapperTypes() {
		Set<Class<?>> ret = new HashSet<Class<?>>();
		ret.add(Boolean.class);
		ret.add(Character.class);
		ret.add(Byte.class);
		ret.add(Short.class);
		ret.add(Integer.class);
		ret.add(Long.class);
		ret.add(Float.class);
		ret.add(Double.class);
		ret.add(Void.class);
		return ret;
	}


	private static Set<String> getInternalType() {
		Set<String> ret = new HashSet<String>();
		ret.add("boolean");
		ret.add("character");
		ret.add("byte");
		ret.add("short");
		ret.add("integer");
		ret.add("long");
		ret.add("float");
		ret.add("double");
		ret.add("void");
		ret.add("static");
		ret.add("string");
		ret.add("date");
		ret.add("final");
		ret.add("bigdecimal");
		ret.add("localdatetime");
		ret.add("int");
		ret.add("clob");

		return ret;
	}

	private static Set<String> getTypeList() {
		Set<String> ret = new HashSet<String>();
		ret.add("List<");
		ret.add("Set<");
		return ret;
	}
}

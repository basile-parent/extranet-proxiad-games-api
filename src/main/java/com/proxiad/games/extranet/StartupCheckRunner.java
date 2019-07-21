package com.proxiad.games.extranet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.proxiad.games.extranet.enums.ParameterEnum;
import com.proxiad.games.extranet.enums.TextEnum;
import com.proxiad.games.extranet.model.Parameter;
import com.proxiad.games.extranet.model.Text;
import com.proxiad.games.extranet.repository.ParameterRepository;
import com.proxiad.games.extranet.repository.TextRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StartupCheckRunner implements CommandLineRunner {

	@Autowired
	private ParameterRepository parameterRepository;

	@Autowired
	private TextRepository textRepository;

	@Value("${application.allVideos}")
	private String allVideos;

	@Override
	public void run(String... args) {
		log.info("******************************************");
		log.info("********** Startup check runner **********\n\n\n");

		List<String> unexistingParameters = parameterRepository.findAll().stream()
				.map(Parameter::getKey)
				.filter(parameterKey -> !ParameterEnum.findByKey(parameterKey).isPresent())
				.collect(Collectors.toList());

		if (!unexistingParameters.isEmpty()) {
			log.error("There are missing parameters in database : " + String.join(", ", unexistingParameters));
		}

		manageTrollTexts();

		log.info("******************************************");
		log.info("******************************************");
	}

	private void manageTrollTexts() {
		List<Text> allTexts = textRepository.findAll();
		List<String> allVideosName = Arrays.asList(this.allVideos.split(","));

		allVideosName.stream()
				.filter(videoName -> allTexts.stream().noneMatch(text -> videoName.equals(text.getVideoName())))
				.forEach(videoName -> textRepository.save(Text.builder()
						.videoName(videoName)
						.text("")
						.voice("French Female")
						.discriminant(TextEnum.TROLL)
						.build()));

		allTexts.stream()
				.filter(text -> text.getDiscriminant().equals(TextEnum.TROLL))
				.filter(text -> allVideosName.stream().noneMatch(videoName -> videoName.equals(text.getVideoName())))
				.forEach(text -> textRepository.delete(text));

		allTexts.stream().filter(text -> text.getDiscriminant().equals(TextEnum.TROLL_END))
				.findAny()
				.orElseGet(() -> textRepository.save(Text.builder()
						.text("")
						.voice("French Female")
						.discriminant(TextEnum.TROLL_END)
						.build()));

		allTexts.stream().filter(text -> text.getDiscriminant().equals(TextEnum.LAST_ENIGMA))
				.findAny()
				.orElseGet(() -> textRepository.save(Text.builder()
						.text("")
						.voice("French Female")
						.discriminant(TextEnum.LAST_ENIGMA)
						.build()));

		allTexts.stream().filter(text -> text.getDiscriminant().equals(TextEnum.OPEN_TERMINAL))
				.findAny()
				.orElseGet(() -> textRepository.save(Text.builder()
						.text("")
						.voice("French Female")
						.discriminant(TextEnum.OPEN_TERMINAL)
						.build()));
	}

}

package com.proxiad.games.extranet.service;

import com.proxiad.games.extranet.dto.RoomDto;
import com.proxiad.games.extranet.enums.MandatoryParameter;
import com.proxiad.games.extranet.enums.RiddleType;
import com.proxiad.games.extranet.mapper.RoomMapper;
import com.proxiad.games.extranet.model.Parameter;
import com.proxiad.games.extranet.model.Riddle;
import com.proxiad.games.extranet.model.Room;
import com.proxiad.games.extranet.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomService {

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private RoomMapper roomMapper;

	@Autowired
	private RiddleRepository riddleRepository;

	@Autowired
	private ParameterRepository parameterRepository;

	public List<RoomDto> findAll() {
		return roomRepository.findAll().stream()
				.map(room -> roomMapper.toDto(room))
				.collect(Collectors.toList());
	}

	public Room newRoom() {
		Room room = new Room();
		room.setName("Nouveau");

		Optional<Parameter> defaultVolumeParameter = parameterRepository.findByKey(MandatoryParameter.AUDIO_BACKGROUND_DEFAULT_VOLUME.getKey());
		Double defaultAudioVolume = Double.valueOf(
				defaultVolumeParameter.orElse(Parameter.builder()
						.key(MandatoryParameter.AUDIO_BACKGROUND_DEFAULT_VOLUME.getKey())
						.value(MandatoryParameter.AUDIO_BACKGROUND_DEFAULT_VOLUME.getDefaultValue())
						.type(MandatoryParameter.AUDIO_BACKGROUND_DEFAULT_VOLUME.getType())
						.build())
						.getValue());
		room.setAudioBackgroundVolume(defaultAudioVolume);
		room = roomRepository.save(room);

		Riddle openDoorRiddle = Riddle.builder()
				.type(RiddleType.OPEN_DOOR)
				.name("Open door")
				.riddleId("open_door")
				.resolved(false)
				.room(room)
				.build();

		riddleRepository.save(openDoorRiddle);

		room.setRiddles(Collections.singletonList(openDoorRiddle));

		return room;
	}

	public Room reinitRoom(Integer roomId) {
		Room room = roomRepository.findById(roomId)
				.orElseThrow(() -> new EntityNotFoundException("No room with id " + roomId));
		room.getRiddles().forEach(riddle -> {
			riddle.setResolved(false);
			riddleRepository.save(riddle);
		});
		room.setTimer(null);
		room.setIsTerminated(false);
		room.setTerminateStatus(null);
		room.setTrollIndex(0);

		return roomRepository.save(room);
	}

	public void deleteRoom(Integer roomId) {
		Room room = roomRepository.findById(roomId)
				.orElseThrow(() -> new EntityNotFoundException("No room with id " + roomId));
		roomRepository.delete(room);
	}

}

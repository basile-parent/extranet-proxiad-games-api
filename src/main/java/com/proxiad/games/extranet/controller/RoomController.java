package com.proxiad.games.extranet.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import com.proxiad.games.extranet.annotation.AdminTokenSecurity;
import com.proxiad.games.extranet.annotation.BypassSecurity;
import com.proxiad.games.extranet.dto.RoomDto;
import com.proxiad.games.extranet.dto.RoomTrollDto;
import com.proxiad.games.extranet.enums.MandatoryParameter;
import com.proxiad.games.extranet.enums.TextEnum;
import com.proxiad.games.extranet.exception.ProxiadControllerException;
import com.proxiad.games.extranet.mapper.RoomMapper;
import com.proxiad.games.extranet.model.Parameter;
import com.proxiad.games.extranet.model.Room;
import com.proxiad.games.extranet.model.Text;
import com.proxiad.games.extranet.model.Timer;
import com.proxiad.games.extranet.repository.ParameterRepository;
import com.proxiad.games.extranet.repository.RoomRepository;
import com.proxiad.games.extranet.repository.TextRepository;
import com.proxiad.games.extranet.service.RoomService;

@RestController
@CrossOrigin
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TextRepository textRepository;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private ParameterRepository parameterRepository;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @GetMapping("/room")
    @AdminTokenSecurity
    public List<RoomDto> listAllRooms() {
        return roomService.findAll();
    }

    @PutMapping(value = "/room")
    @AdminTokenSecurity
    public ResponseEntity<?> newRoom() {
        Room room = new Room();
        room.setName("Nouveau");
        roomRepository.save(room);

        return new ResponseEntity<>(room, HttpStatus.OK);
    }

    @PostMapping(value = "/room/{id}/name")
    @AdminTokenSecurity
    public ResponseEntity<?> updateRoomName(@PathVariable("id") Integer id, @RequestBody RoomDto updatedRoom) {
        Optional<Room> optRoom = roomRepository.findById(id);

        if (!optRoom.isPresent()) {
            return new ResponseEntity<>("No room with id " + id, HttpStatus.BAD_REQUEST);
        }

        Room room = optRoom.get();
        room.setName(updatedRoom.getName());
        roomRepository.save(room);

        return new ResponseEntity<>(room, HttpStatus.OK);
    }

    @DeleteMapping(value = "/room/{id}")
    @AdminTokenSecurity
    public ResponseEntity<?> deleteRoom(@PathVariable("id") Integer id) {
        Optional<Room> optRoom = roomRepository.findById(id);

        if (!optRoom.isPresent()) {
            return new ResponseEntity<>("No room with id " + id, HttpStatus.BAD_REQUEST);
        }

        roomRepository.delete(optRoom.get());

        return new ResponseEntity<>("deleted", HttpStatus.OK);
    }

    @PatchMapping(value = "/room/{id}/reinit")
    @AdminTokenSecurity
    public ResponseEntity<?> reinitRoom(@PathVariable("id") Integer id) {
        Optional<Room> optRoom = roomRepository.findById(id);
        if (!optRoom.isPresent()) {
            return new ResponseEntity<>("No room with id " + id, HttpStatus.BAD_REQUEST);
        }

        Room room = optRoom.get();
        room.setResolvedRiddles(new ArrayList<>());
        room.setTimer(null);
        room.setIsTerminated(false);
        room.setTerminateStatus(null);
        room.setTrollIndex(0);
        roomRepository.save(room);

        this.simpMessagingTemplate.convertAndSend("/topic/room/" + room.getId() + "/reinit", new RoomDto());

        return new ResponseEntity<>(roomMapper.toDto(room), HttpStatus.OK);
    }

    @RequestMapping("/user/troll")
    @BypassSecurity
    public void troll(@RequestParam("salle") String roomName) throws ProxiadControllerException {
        final Optional<Room> optRoom = roomRepository.findByNameIgnoreCase(roomName);
        if (!optRoom.isPresent()) {
            throw new ProxiadControllerException("Your room is unknown. Please contact the administrator.");
        }

        final Room room = optRoom.get();

        Optional<Parameter> decreaseTimeParameter = parameterRepository.findByKey(MandatoryParameter.TROLL_DECREASE_TIME.getKey());
        Integer decreaseTime = Integer.parseInt(decreaseTimeParameter.orElse(
                Parameter.builder()
                        .key(MandatoryParameter.TROLL_DECREASE_TIME.getKey())
                        .value(MandatoryParameter.TROLL_DECREASE_TIME.getDefaultValue())
                        .type(MandatoryParameter.TROLL_DECREASE_TIME.getType()
                        )
                        .build())
                .getValue());

        final List<Text> trollTexts = textRepository.findAllByDiscriminantOrderByIdAsc(TextEnum.TROLL);

        final Timer timer = Optional.ofNullable(room.getTimer()).orElseThrow(() -> new ProxiadControllerException("No timer found for the room " + room.getName()));
        timer.setRemainingTime(Math.max(0, timer.getRemainingTime() - decreaseTime));
        room.setTimer(timer);
        final Integer trollIndex = room.getTrollIndex();
        final Integer newTrollIndex = trollIndex + 1 >= trollTexts.size() ? trollTexts.size() - 1 : trollIndex + 1;
        room.setTrollIndex(newTrollIndex);
        roomRepository.save(room);

        final Text trollText = trollTexts.get(trollIndex);
        final RoomTrollDto roomTrollDto = RoomTrollDto.builder()
                .id(room.getId())
                .name(room.getName())
                .reduceTime(decreaseTime)
                .message(trollText.getText())
                .voice(trollText.getVoice())
                .videoName(trollText.getVideoName())
                .build();

        this.simpMessagingTemplate.convertAndSend("/topic/room/admin/troll", roomTrollDto);
        this.simpMessagingTemplate.convertAndSend("/topic/room/" + room.getId() + "/troll", roomTrollDto);
    }

}

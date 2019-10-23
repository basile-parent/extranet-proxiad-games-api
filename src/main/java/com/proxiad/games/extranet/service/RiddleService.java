package com.proxiad.games.extranet.service;

import com.proxiad.games.extranet.enums.RiddleType;
import com.proxiad.games.extranet.model.Riddle;
import com.proxiad.games.extranet.model.Room;
import com.proxiad.games.extranet.repository.RiddleRepository;
import com.proxiad.games.extranet.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;

@Service
public class RiddleService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RiddleRepository riddleRepository;

    public Riddle newRiddle(Integer roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("No room with id " + roomId));

        return riddleRepository.save(Riddle.builder()
                .name("Nouveau")
                .riddleId("idToSet")
                .room(room)
                .resolved(false)
                .type(RiddleType.GAME)
                .build());
    }

}
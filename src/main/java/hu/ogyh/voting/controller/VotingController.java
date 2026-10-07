package hu.ogyh.voting.controller;

import hu.ogyh.voting.dto.request.SaveVotingRequest;
import hu.ogyh.voting.dto.response.DailyVotingsResponse;
import hu.ogyh.voting.dto.response.MemberVoteResponse;
import hu.ogyh.voting.dto.response.ParticipationAverageResponse;
import hu.ogyh.voting.dto.response.ResultResponse;
import hu.ogyh.voting.dto.response.SaveVotingResponse;
import hu.ogyh.voting.dto.response.SpecialProceduresResponse;
import hu.ogyh.voting.service.VotingService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 2.2 Végpontok: csak HTTP-fordítás, az üzleti döntések a {@link VotingService}-ben vannak. */
@RestController
@RequestMapping("/szavazasok")
@RequiredArgsConstructor
public class VotingController {

    private final VotingService votingService;

    /** 1. Szavazás mentése. */
    @PostMapping("/szavazas")
    public SaveVotingResponse save(@Valid @RequestBody SaveVotingRequest request) {
        return votingService.save(request);
    }

    /** 2. Képviselő szavazata. */
    @GetMapping("/szavazat")
    public MemberVoteResponse getVote(
            @RequestParam("szavazas") String publicId, @RequestParam("kepviselo") String memberId) {
        return votingService.getVote(publicId, memberId);
    }

    /** 3. Szavazás eredménye. */
    @GetMapping("/eredmeny")
    public ResultResponse getResult(@RequestParam("szavazas") String publicId) {
        return votingService.getResult(publicId);
    }

    /** 4. Napi szavazások. */
    @GetMapping("/napi-szavazasok")
    public DailyVotingsResponse getDailyVotings(@RequestParam("nap") LocalDate day) {
        return votingService.getDailyVotings(day);
    }

    /** 5.1 Részvételi átlag. */
    @GetMapping("/kepviselo-reszvetel-atlag")
    public ParticipationAverageResponse getParticipationAverage(
            @RequestParam("tol") LocalDate firstDay, @RequestParam("ig") LocalDate lastDay) {
        return votingService.getParticipationAverage(firstDay, lastDay);
    }

    /** 5.2 Különleges eljárások. */
    @GetMapping("/kulonleges-eljarasok-szama")
    public SpecialProceduresResponse countSpecialProcedures(
            @RequestParam("tol") LocalDate firstDay, @RequestParam("ig") LocalDate lastDay) {
        return votingService.countSpecialProcedures(firstDay, lastDay);
    }
}

package com.ururulab.ururu.member.service;

import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.member.dto.request.MemberRequest;

import java.lang.reflect.Field;
import java.time.Instant;

public class MemberTestFixture {

    public static SocialMemberInfo createSocialMemberInfo() {
        return new SocialMemberInfo(
                "testuser",
                "test@example.com",
                "social123",
                "",
                SocialProvider.GOOGLE
        );
    }

    public static Member createMember(Long id, String nickname, String email){
        Member member =  Member.of(
                nickname,
                email,
                SocialProvider.GOOGLE,
                "social123",
                Gender.FEMALE,
                Instant.parse("1990-01-01T00:00:00Z"),
                "01012345678",
                null,
                Role.NORMAL
        );
        setMemberId(member, id);
        return member;
    }

    public static MemberRequest createMemberUpdateRequest(String nickname, String phone) {
        return new MemberRequest(
                nickname,
                "updated@example.com",
                SocialProvider.GOOGLE,
                "social123",
                "MALE",
                Instant.parse("1985-01-01T00:00:00Z"),
                phone,
                null
        );
    }



    private static void setMemberId(Member member, Long id){
        try{
            Field idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(member, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set member id for test", e);
        }
    }
}

package qorhvkdy.qorhvkdy.rpgmod.weapon;

/**
 * 무기 옵션 구조화 모델.
 * mode/stat/value 3요소를 표준화해 옵션 계산기로 확장하기 쉽다.
 */
public record WeaponOptionDescriptor(
        String mode,
        String stat,
        double value,
        String label
) {
}


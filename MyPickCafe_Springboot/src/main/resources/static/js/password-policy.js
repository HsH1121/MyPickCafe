/*
 * 비밀번호 입력 안내 — 서버 PasswordPolicy 의 길이 규칙을 입력 중에 미리 알려 준다.
 * 최종 판단(흔한 비밀번호 차단 등)은 서버가 한다. 여기서는 길이만 본다.
 *
 * 사용: <input type="password" data-password-policy> 와
 *       <p data-password-policy-hint="입력 id"> (선택), <input data-password-confirm="새 비밀번호 입력 id"> (선택)
 *
 * 기준(서버와 동일)
 *   - 글자 수: 유니코드 코드 포인트 기준 12자 이상 64자 이하 (Array.from(value).length)
 *   - UTF-8 72바이트 이하 (BCrypt 한도. 한글은 글자당 3바이트)
 *   - 공백은 허용하며 잘라내지 않는다. 공백만으로는 안 된다.
 */
(function () {
  var MIN = 12, MAX = 64, MAX_BYTES = 72;

  function utf8Bytes(s) {
    return new TextEncoder().encode(s).length;
  }

  function check(value, optional) {
    if (!value) return optional ? '' : '비밀번호를 입력해주세요.';
    if (!value.trim()) return '공백만으로는 비밀번호를 만들 수 없습니다.';
    var len = Array.from(value).length;
    if (len < MIN) return '비밀번호는 ' + MIN + '자 이상이어야 합니다. (현재 ' + len + '자)';
    if (len > MAX) return '비밀번호는 ' + MAX + '자 이하여야 합니다.';
    if (utf8Bytes(value) > MAX_BYTES) return '비밀번호가 너무 깁니다. 영문·숫자는 64자, 한글은 24자까지 쓸 수 있습니다.';
    if (/[\u0000-\u001f\u007f  ]/.test(value)) return '줄바꿈·탭 같은 제어 문자는 쓸 수 없습니다.';
    return '';
  }

  function bind(input) {
    var optional = input.hasAttribute('data-password-optional');
    var hint = document.querySelector('[data-password-policy-hint="' + input.id + '"]');
    var defaultHint = hint ? hint.textContent : '';
    function update() {
      var msg = check(input.value, optional);
      input.setCustomValidity(msg);
      if (hint) {
        hint.textContent = msg || defaultHint;
        hint.style.color = msg && input.value ? 'var(--danger)' : '';
      }
    }
    input.addEventListener('input', update);
    update();
  }

  function bindConfirm(confirm) {
    var target = document.getElementById(confirm.getAttribute('data-password-confirm'));
    if (!target) return;
    function update() {
      confirm.setCustomValidity(confirm.value && confirm.value !== target.value ? '비밀번호가 일치하지 않습니다.' : '');
    }
    confirm.addEventListener('input', update);
    target.addEventListener('input', update);
  }

  document.querySelectorAll('input[data-password-policy]').forEach(bind);
  document.querySelectorAll('input[data-password-confirm]').forEach(bindConfirm);
})();

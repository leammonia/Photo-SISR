const slider = document.querySelector('[data-slider]');
var timer = 300;

slider.addEventListener('input', e => {
  timer = 500 - slider.value;
});

addCircles();

function addCircles() {
  setTimeout(() => {
    addCircle(...randomPosition());
    addCircles();
  }, timer);
}

function addCircle(x, y) {
  var circle = document.createElement('div');
  var animationTime = Math.round(Math.random() * 10);
  circle.classList.add('circle');
  circle.style.left = x + 'vw';
  circle.style.top = y + 'vh';
  circle.style.backgroundColor = randomColor();
  circle.style.setProperty('--grow-time', `${animationTime}s`);

  requestAnimationFrame(() => {
    document.body.appendChild(circle);
    setTimeout(removeCircle.bind(this, circle), animationTime * 1000);
  });
}

function removeCircle(circle) {
  document.body.removeChild(circle);
}

function randomPosition() {
  return [
  Math.random() * 100 + 1,
  Math.random() * 100 + 1];

}

function randomColor() {
  const colors = [
  '#69D2E7', '#A7DBD8', '#E0E4CC', '#F38630', '#FA6900',
  '#FF4E50', '#F9D423'];


  return colors[Math.round(Math.random() * colors.length)];
}